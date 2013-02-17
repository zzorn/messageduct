package org.messageduct.server;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
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
import org.messageduct.protocol.BinaryProtocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Handles server side networking.
 */
public class ServerNetworking {

    private final Authenticator authenticator;
    private final int port;
    private final int bufferSize;
    private final int idleTimeSeconds;

    private IoAcceptor acceptor;
    private ThreadPoolExecutor executor;
    private final BlacklistFilter blacklistFilter = new BlacklistFilter();


    /**
     * Creates a new server networking handler, with a 2kb buffer and a half minute idle time.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     */
    public ServerNetworking(Authenticator authenticator, int port) {
        this(authenticator, port, 2048);
    }

    /**
     * Creates a new server networking handler with a half minute idle time.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param bufferSize Input buffer size.
     */
    public ServerNetworking(Authenticator authenticator, int port, int bufferSize) {
        this(authenticator, port, bufferSize, 30);
    }

    /**
     * Creates a new server networking handler.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param bufferSize Input buffer size.
     * @param idleTimeSeconds time before connection handlers are notified that the connection is idle.
     */
    public ServerNetworking(Authenticator authenticator, int port, int bufferSize, int idleTimeSeconds) {
        this.authenticator = authenticator;
        this.port = port;
        this.bufferSize = bufferSize;
        this.idleTimeSeconds = idleTimeSeconds;
    }

    /**
     * Starts listening to the port specified in the constructor, and handling client connections.
     * @throws Exception if there was a problem binding to the port, or some other issue.
     */
    public void start() throws Exception {
        if (acceptor != null) throw new IllegalStateException("Already started");

        acceptor = new NioSocketAcceptor();
        executor = createThreadPool();

        // Blacklist
        acceptor.getFilterChain().addLast("blacklist", blacklistFilter);

        // Slow down new connections
        acceptor.getFilterChain().addLast("connectionThrottle", new ConnectionThrottleFilter());

        // Execute incoming messages in threads from the thread pool.
        acceptor.getFilterChain().addLast("executor", new ExecutorFilter(executor));

        // Encrypt/decrypt
        SslContextFactory sslContextFactory = new SslContextFactory();
        final SslFilter sslFilter = new SslFilter(sslContextFactory.newInstance());
        sslFilter.setUseClientMode(false);
        sslFilter.setNeedClientAuth(true);
        acceptor.getFilterChain().addLast("encryption", sslFilter);

        // Compress/decompress
        acceptor.getFilterChain().addLast("compress", new CompressionFilter());

        // Decode traffic
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new BinaryProtocol()));

        // Ensure messages are authenticated
        acceptor.getFilterChain().addLast("authentication", new AuthenticationFilter(authenticator));

        // Set handler class that handles incoming messages and events.
        acceptor.setHandler(createConnectionHandler());

        // Set buffer size used for incoming messages
        acceptor.getSessionConfig().setReadBufferSize(bufferSize);

        // Set time after witch idle is called on the connection handler.
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, idleTimeSeconds);

        // Listen to the specified port
        acceptor.bind(new InetSocketAddress(port));
    }

    /**
     * Unbinds the server from the port and shuts down the thread pool handling client connections.
     */
    public void shutdown() {
        if (acceptor != null) acceptor.unbind();
        if (executor != null) executor.shutdown();
    }

    /**
     * Blacklists a specific ip.
     * A banned ip will be immediately disconnected if it tries to connect.
     */
    public void banIp(InetAddress address) {
        blacklistFilter.block(address);
    }

    /**
     * Removes blacklisting of a previously blacklisted ip.
     * (If the subnet that the ip is in is blacklisted, the ip will still be blacklisted).
     */
    public void unBanIp(InetAddress address) {
        blacklistFilter.unblock(address);
    }

    /**
     * Blacklists a whole subnet.
     * An ip from a banned subnet will be immediately disconnected if it tries to connect.
     */
    public void banSubnet(Subnet subnet) {
        blacklistFilter.block(subnet);
    }

    /**
     * Removes blacklisting of a previously blacklisted subnet.
     * If there are other blacklistings affecting parts of the subnet, they are not removed.
     */
    public void unBanSubnet(Subnet subnet) {
        blacklistFilter.unblock(subnet);
    }

    protected IoHandler createConnectionHandler() {
        return new ConnectionHandler();
    }

    protected ThreadPoolExecutor createThreadPool() {
        return new OrderedThreadPoolExecutor();
    }

}
