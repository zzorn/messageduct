package org.messageduct.server.minaserver;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.filter.firewall.BlacklistFilter;
import org.apache.mina.filter.firewall.ConnectionThrottleFilter;
import org.apache.mina.filter.firewall.Subnet;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.messageduct.account.AccountService;
import org.messageduct.account.DefaultAccountService;
import org.messageduct.account.persistence.MapDBAccountPersistence;
import org.messageduct.common.NetworkConfig;
import org.messageduct.server.MessageListener;
import org.messageduct.server.ServerNetworking;
import org.messageduct.utils.service.ServiceBase;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 * Handles server side networking using the Apache Mina library.
 */
public class MinaServerNetworking extends ServiceBase implements ServerNetworking {

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final int DEFAULT_IDLE_TIME_SECONDS = 30;

    private final int idleTimeSeconds;
    private final int bufferSize;

    private final NetworkConfig networkConfig;
    private final AccountService accountService;

    private IoAcceptor acceptor;
    private OrderedThreadPoolExecutor executor;
    private final BlacklistFilter blacklistFilter = new BlacklistFilter();
    private final ConnectionHandler connectionHandler = new ConnectionHandler();


    /**
     * Creates a new server networking handler, with a default buffer size, idle time, and account service.
     *
     * @param networkConfig connection specific configuration.
     * @param accountDatabaseFile file to store the account database in.
     */
    public MinaServerNetworking(NetworkConfig networkConfig, File accountDatabaseFile) {
        this(networkConfig, new DefaultAccountService(new MapDBAccountPersistence(accountDatabaseFile)));
    }


    /**
     * Creates a new server networking handler, with a default buffer size and a idle time.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     */
    public MinaServerNetworking(NetworkConfig networkConfig, AccountService accountService) {
        this(networkConfig, accountService, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a new server networking handler, with a default buffer size.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     * @param bufferSize Input buffer size.
     */
    public MinaServerNetworking(NetworkConfig networkConfig, AccountService accountService, int bufferSize) {
        this(networkConfig, accountService, bufferSize, DEFAULT_IDLE_TIME_SECONDS);
    }

    /**
     * Creates a new server networking handler.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     * @param bufferSize Input buffer size.
     * @param idleTimeSeconds time before connection handlers are notified that the connection is idle.
     */
    public MinaServerNetworking(NetworkConfig networkConfig,
                                AccountService accountService,
                                int bufferSize,
                                int idleTimeSeconds) {
        notNull(networkConfig, "networkConfig");
        notNull(accountService, "accountService");
        positive(bufferSize, "bufferSize");
        positive(idleTimeSeconds, "idleTimeSeconds");

        this.networkConfig = networkConfig;
        this.accountService = accountService;
        this.bufferSize = bufferSize;
        this.idleTimeSeconds = idleTimeSeconds;
    }

    @Override public final void addMessageListener(MessageListener listener) {
        connectionHandler.addListener(listener);
    }

    @Override public final void removeMessageListener(MessageListener listener) {
        connectionHandler.removeListener(listener);
    }

    @Override protected void doInit() {
        // Setup acceptor that will handle incoming connection attempts
        acceptor = new NioSocketAcceptor();
        // Set buffer size used for incoming messages
        acceptor.getSessionConfig().setReadBufferSize(bufferSize);
        // Set time after witch idle is called on the connection handler.
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, idleTimeSeconds);

        // Set up chain of filters to apply to incoming and outgoing messages
        initializeFilterChain(acceptor.getFilterChain());

        // Set handler that handles incoming messages and events.
        acceptor.setHandler(connectionHandler);

        // Listen to the specified port
        try {
            acceptor.bind(new InetSocketAddress(networkConfig.getPort()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not bind to the port "+networkConfig.getPort()+": " + e.getMessage(), e);
        }
    }

    private void initializeFilterChain(DefaultIoFilterChainBuilder filterChain) {
        // Blacklist
        filterChain.addLast("blacklist", blacklistFilter);

        // Limit rate of new connections from a single source
        filterChain.addLast("connectionThrottle", new ConnectionThrottleFilter());

        // Execute incoming messages in threads from the thread pool.
        // (Note that this is not applied for outgoing messages)
        executor = createThreadPool();
        filterChain.addLast("executor", new ExecutorFilter(executor));

        if (networkConfig.isEncryptionEnabled()) {
            // Encrypt/decrypt traffic on the connection
            SslContextFactory sslContextFactory = new SslContextFactory();
            final SslFilter sslFilter;
            try {
                sslFilter = new SslFilter(sslContextFactory.newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Could not create encryption filter for networking: " + e.getMessage(), e);
            }
            sslFilter.setUseClientMode(false);
            sslFilter.setNeedClientAuth(true);
            filterChain.addLast("encryption", sslFilter);
        }

        if (networkConfig.isCompressionEnabled()) {
            // Compress/decompress traffic
            filterChain.addLast("compress", new CompressionFilter());
        }

        // Encode/Decode traffic between Java Objects and binary data
        filterChain.addLast("codec", new ProtocolCodecFilter(new SerializerProtocol(networkConfig.getSerializer())));

        // Ensure authentication is done when a connection is initialized before passing messages on
        // Also handles account related actions such as password resets or email changes.
        filterChain.addLast("authentication", new AuthenticationFilter(accountService));
    }

    @Override protected void doShutdown() {
        // Disconnect from port, and close all connections
        if (acceptor != null) acceptor.unbind();

        // Shut down thread pool handling connections
        if (executor != null) executor.shutdown();
    }

    @Override public final void banIp(InetAddress address) {
        blacklistFilter.block(address);
    }

    @Override public final void unBanIp(InetAddress address) {
        blacklistFilter.unblock(address);
    }

    @Override public final void banSubnet(Subnet subnet) {
        blacklistFilter.block(subnet);
    }

    @Override public final void unBanSubnet(Subnet subnet) {
        blacklistFilter.unblock(subnet);
    }

    /**
     * @return the ordered thread pool that will be used to run client connection handlers.
     *         Called once when the networking service is started.
     */
    protected OrderedThreadPoolExecutor createThreadPool() {
        return new OrderedThreadPoolExecutor();
    }

}
