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
import org.flowutils.Check;
import org.messageduct.account.AccountService;
import org.messageduct.protocol.BinaryProtocol;
import org.messageduct.server.MessageListener;
import org.messageduct.server.ServerNetworking;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Handles server side networking using the Apache Mina library.
 */
public class MinaServerNetworking implements ServerNetworking {

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final int DEFAULT_IDLE_TIME_SECONDS = 30;

    private int port;
    private int idleTimeSeconds;
    private final int bufferSize;
    private final Set<Class> allowedClasses = new HashSet<Class>();

    private AccountService accountService;

    private IoAcceptor acceptor;
    private OrderedThreadPoolExecutor executor;
    private final BlacklistFilter blacklistFilter = new BlacklistFilter();
    private final ConnectionHandler connectionHandler = new ConnectionHandler();


    /**
     * Creates a new server networking handler, with a 8kb buffer and a half minute idle time.
     * Remember to add any classes that should be allowed to be transferred with registerAllowedClass.
     * @param port port that the server should listen at.
     */
    public MinaServerNetworking(int port) {
        this(null, port, null);
    }

    /**
     * Creates a new server networking handler, with a 8kb buffer and a half minute idle time.
     * Remember to add any classes that should be allowed to be transferred with registerAllowedClass.
     * @param accountService used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     */
    public MinaServerNetworking(AccountService accountService, int port) {
        this(accountService, port, null);
    }

    /**
     * Creates a new server networking handler, with a 8kb buffer and a half minute idle time.
     * @param accountService used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
     *                       Primitive and wrapper classes are allowed by default.
     *                       NOTE: It's important that the server and client have exactly the same allowed classes!
     */
    public MinaServerNetworking(AccountService accountService, int port, final Set<Class> allowedClasses) {
        this(accountService, port, allowedClasses, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a new server networking handler with a half minute idle time.
     * @param accountService used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
     *                       Primitive and wrapper classes are allowed by default.
     *                       NOTE: It's important that the server and client have exactly the same allowed classes!
     * @param bufferSize Input buffer size.
     */
    public MinaServerNetworking(AccountService accountService, int port, final Set<Class> allowedClasses, int bufferSize) {
        this(accountService, port, allowedClasses, bufferSize, DEFAULT_IDLE_TIME_SECONDS);
    }

    /**
     * Creates a new server networking handler.
     * @param accountService used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
     *                       Primitive and wrapper classes are allowed by default.
     *                       NOTE: It's important that the server and client have exactly the same allowed classes!
     * @param bufferSize Input buffer size.
     * @param idleTimeSeconds time before connection handlers are notified that the connection is idle.
     */
    public MinaServerNetworking(AccountService accountService,
                                int port,
                                final Set<Class> allowedClasses,
                                int bufferSize,
                                int idleTimeSeconds) {
        Check.positive(bufferSize, "bufferSize");

        this.accountService = accountService;
        this.bufferSize = bufferSize;
        setPort(port);
        setIdleTimeSeconds(idleTimeSeconds);
        if (allowedClasses != null) this.allowedClasses.addAll(allowedClasses);
    }

    @Override public final void setAccountService(AccountService accountService) {
        checkNotStarted("Setting an accountService");
        Check.notNull(accountService, "accountService");

        this.accountService = accountService;
    }

    @Override public final AccountService getAccountService() {
        return accountService;
    }

    @Override public final void addMessageListener(MessageListener listener) {
        connectionHandler.addListener(listener);
    }

    @Override public final void removeMessageListener(MessageListener listener) {
        connectionHandler.removeListener(listener);
    }

    @Override public final void registerAllowedClass(Class aClass) {
        checkNotStarted("Registering new allowed classes");
        Check.notNull(aClass, "aClass");

        allowedClasses.add(aClass);
    }

    @Override public final void registerAllowedClasses(Class... classes) {
        for (Class aClass : classes) {
            registerAllowedClass(aClass);
        }
    }

    @Override public final void registerAllowedClasses(Set<Class> classes) {
        Check.notNull(classes, "classes");

        for (Class aClass : classes) {
            registerAllowedClass(aClass);
        }
    }

    @Override public final Set<Class> getAllowedClasses() {
        return Collections.unmodifiableSet(allowedClasses);
    }

    @Override public final int getPort() {
        return port;
    }

    @Override public final void setPort(int port) {
        checkNotStarted("Setting the server port");
        Check.positive(port, "port");
        this.port = port;
    }

    @Override public final int getIdleTimeSeconds() {
        return idleTimeSeconds;
    }

    @Override public final void setIdleTimeSeconds(int idleTimeSeconds) {
        checkNotStarted("Setting the idletime");
        Check.positive(idleTimeSeconds, "idleTimeSeconds");
        this.idleTimeSeconds = idleTimeSeconds;
    }

    @Override public final void start() throws Exception {
        if (isStarted()) throw new IllegalStateException("Networking has already been started, can not start again.");

        if (accountService == null) throw new IllegalStateException("No accountService specified.  Set one in the constructor or with setAccountService");
        allowedClasses.addAll(accountService.getHandledMessageTypes());
        allowedClasses.addAll(accountService.getOtherAcceptedClasses());

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
        acceptor.bind(new InetSocketAddress(port));
    }

    private void initializeFilterChain(DefaultIoFilterChainBuilder filterChain) throws Exception {
        // Blacklist
        filterChain.addLast("blacklist", blacklistFilter);

        // Limit rate of new connections from a single source
        filterChain.addLast("connectionThrottle", new ConnectionThrottleFilter());

        // Execute incoming messages in threads from the thread pool.
        // (Note that this is not applied for outgoing messages)
        executor = createThreadPool();
        filterChain.addLast("executor", new ExecutorFilter(executor));

        // Encrypt/decrypt traffic on the connection
        SslContextFactory sslContextFactory = new SslContextFactory();
        final SslFilter sslFilter = new SslFilter(sslContextFactory.newInstance());
        sslFilter.setUseClientMode(false);
        sslFilter.setNeedClientAuth(true);
        filterChain.addLast("encryption", sslFilter);

        // Compress/decompress traffic
        filterChain.addLast("compress", new CompressionFilter());

        // Encode/Decode traffic between Java Objects and binary data
        filterChain.addLast("codec", new ProtocolCodecFilter(createProtocol(allowedClasses, bufferSize)));

        // Ensure authentication is done when a connection is initialized before passing messages on
        // Also handles account related actions such as password resets or email changes.
        filterChain.addLast("authentication", new AuthenticationFilter(accountService));
    }

    @Override public final boolean isStarted() {
        return acceptor != null;
    }

    @Override public final void shutdown() {
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
     * @param allowedClasses classes that are allowed to be sent over the network.
     * @param inputBufferSize max size for any input buffer used by the protocol.
     * @return the protocol used to convert between stream data and Java objects.
     */
    protected BinaryProtocol createProtocol(final Set<Class> allowedClasses, final int inputBufferSize) {
        return new BinaryProtocol(allowedClasses, inputBufferSize);
    }

    /**
     * @return the ordered thread pool that will be used to run client connection handlers.
     *         Called once when the networking service is started.
     */
    protected OrderedThreadPoolExecutor createThreadPool() {
        return new OrderedThreadPoolExecutor();
    }


    private void checkNotStarted(final String action) {
        if (isStarted()) throw new IllegalStateException(action +" is not permitted after start has been called.");
    }

}
