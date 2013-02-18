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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Handles server side networking.
 */
public class ServerNetworking {

    private final Authenticator authenticator;
    private final int port;
    private final int bufferSize;
    private final int idleTimeSeconds;
    private final List<Class> allowedClasses = new ArrayList<Class>();

    private IoAcceptor acceptor;
    private ThreadPoolExecutor executor;
    private final BlacklistFilter blacklistFilter = new BlacklistFilter();


    /**
     * Creates a new server networking handler, with a 8kb buffer and a half minute idle time.
     * Remember to add any classes that should be allowed to be transferred with registerAllowedClass.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     */
    public ServerNetworking(Authenticator authenticator, int port) {
        this(authenticator, port, Collections.<Class>emptyList());
    }

    /**
     * Creates a new server networking handler, with a 8kb buffer and a half minute idle time.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
     *                       Primitive and wrapper classes are allowed by default.
     */
    public ServerNetworking(Authenticator authenticator, int port, final List<Class> allowedClasses) {
        this(authenticator, port, allowedClasses, 8 * 1024);
    }

    /**
     * Creates a new server networking handler with a half minute idle time.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
 *                       Primitive and wrapper classes are allowed by default.
     * @param bufferSize Input buffer size.
     */
    public ServerNetworking(Authenticator authenticator, int port, final List<Class> allowedClasses, int bufferSize) {
        this(authenticator, port, allowedClasses, bufferSize, 30);
    }

    /**
     * Creates a new server networking handler.
     * @param authenticator used for authenticating users and creating new accounts.
     * @param port port that the server should listen at.
     * @param allowedClasses the classes that are allowed to be transferred over the connection and instantiated.
 *                       Primitive and wrapper classes are allowed by default.
     * @param bufferSize Input buffer size.
     * @param idleTimeSeconds time before connection handlers are notified that the connection is idle.
     */
    public ServerNetworking(Authenticator authenticator, int port, final List<Class> allowedClasses, int bufferSize, int idleTimeSeconds) {
        this.authenticator = authenticator;
        this.port = port;
        this.bufferSize = bufferSize;
        this.idleTimeSeconds = idleTimeSeconds;
        this.allowedClasses.addAll(allowedClasses);
    }

    /**
     * Adds a class that will be allowed to be transferred over connections created in the future.
     * Should not be called after start is called.
     */
    public void registerAllowedClass(Class aClass) {
        if (isStarted()) throw new IllegalStateException("Registering new allowed classes not permitted after start is called.");

        allowedClasses.add(aClass);
    }

    /**
     * Starts listening to the port specified in the constructor, and handling client connections.
     * @throws Exception if there was a problem binding to the port, or some other issue.
     */
    public void start() throws Exception {
        if (isStarted()) throw new IllegalStateException("Already started");

        executor = createThreadPool();

        // Setup acceptor
        acceptor = new NioSocketAcceptor();
        // Set buffer size used for incoming messages
        acceptor.getSessionConfig().setReadBufferSize(bufferSize);
        // Set time after witch idle is called on the connection handler.
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, idleTimeSeconds);

        // Blacklist
        acceptor.getFilterChain().addLast("blacklist", blacklistFilter);

        // Limit rate of new connections from a single source
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

        // Encode/Decode traffic
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new BinaryProtocol(allowedClasses, bufferSize)));

        // Ensure authentication is done before passing messages on
        acceptor.getFilterChain().addLast("authentication", new AuthenticationFilter(authenticator));

        // Set handler class that handles incoming messages and events.
        acceptor.setHandler(createConnectionHandler());

        // Listen to the specified port
        acceptor.bind(new InetSocketAddress(port));
    }

    /**
     * @return true if this ServerNetworking has been started.
     */
    public boolean isStarted() {
        return acceptor != null;
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
