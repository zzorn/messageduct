package org.messageduct.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.flowutils.service.ServiceBase;
import org.flowutils.service.ServiceProvider;
import org.messageduct.account.AccountService;
import org.messageduct.common.NetworkConfig;
import org.messageduct.common.netty.NettyPipelineBuilder;
import org.messageduct.server.MessageListener;
import org.messageduct.server.ServerNetworking;
import org.messageduct.serverinfo.ServerInfo;
import org.messageduct.utils.banlist.BanList;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.flowutils.Check.notNull;
import static org.messageduct.utils.LoggingUtils.*;

/**
 * Handles server side networking using the Netty library.
 */
public class NettyServerNetworking extends ServiceBase implements ServerNetworking {

    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<MessageListener>();

    private final NetworkConfig networkConfig;
    private final AccountService accountService;
    private final ServerInfo serverInfo;
    private BanList banList;

    private Channel serverChannel;

    /**
     * Creates a new server networking handler.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     *                       Initialized and shutdown by NettyServerNetworking automatically when it is not previously initialized or shutdown.
     * @param serverInfo Information about this server, such as name, description, internet address, and public key.
     */
    public NettyServerNetworking(NetworkConfig networkConfig, AccountService accountService, ServerInfo serverInfo) {
        this(networkConfig, accountService, serverInfo, null);
    }

    /**
     * Creates a new server networking handler.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     *                       Initialized and shutdown by NettyServerNetworking automatically when it is not previously initialized or shutdown.
     * @param listener a listener that is notified about messages received from clients.
     *                 Can be null as well, listeners can be added later with addListener.
     */
    public NettyServerNetworking(NetworkConfig networkConfig, AccountService accountService, ServerInfo serverInfo, MessageListener listener) {
        this(networkConfig, accountService, serverInfo, listener, null);
    }

    /**
     * Creates a new server networking handler.
     *
     * @param networkConfig connection specific configuration.
     * @param accountService used for authenticating users and creating new accounts.
     *                       Initialized and shutdown by NettyServerNetworking automatically when it is not previously initialized or shutdown.
     * @param listener a listener that is notified about messages received from clients.
     *                 Can be null as well, listeners can be added later with addListener.
     * @param banList banlist with IP addresses that are blacklisted from connecting to the server.
     */
    public NettyServerNetworking(NetworkConfig networkConfig, AccountService accountService, ServerInfo serverInfo, MessageListener listener, BanList banList) {
        notNull(networkConfig, "networkConfig");
        notNull(accountService, "accountService");
        notNull(serverInfo, "serverInfo");

        this.networkConfig = networkConfig;
        this.accountService = accountService;
        this.serverInfo = serverInfo;
        this.banList = banList;

        if (listener != null) {
            addMessageListener(listener);
        }
    }

    @Override public final void addMessageListener(MessageListener listener) {
        if (messageListeners.contains(listener)) throw new IllegalArgumentException("Listener already added");

        messageListeners.add(listener);
    }

    @Override public final void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    @Override public final BanList getBanList() {
        return banList;
    }

    @Override public final void setBanList(BanList banList) {
        this.banList = banList;
    }

    @Override protected void doInit(ServiceProvider serviceProvider) {
        // Initialize account service if needed
        if (!accountService.isInitialized()) accountService.init();

        // Configure the server networking
        ServerBootstrap serverBootstrap = createServerBootstrap();

        // Bind and start to accept incoming connections.
        logInfo("Bind to port " + networkConfig.getPort() + ", and start accepting incoming connections");
        try {
            final ChannelFuture channelFuture = serverBootstrap.bind(networkConfig.getPort()).sync();

            // Wait until binding is ready
            channelFuture.await();

            serverChannel = channelFuture.channel();
        } catch (Exception e) {
            logAndThrowError(e, "Server listening to port " + networkConfig.getPort() + " failed");
        }
    }

    @Override protected void doShutdown() {
        // Unbind
        if (serverChannel != null) {
            try {
                serverChannel.flush().close().await();
            } catch (InterruptedException e) {
                log.error("Problem when waiting to close server networking: " + e.getMessage(), e);
                serverChannel.close();
            }
        }

    }

    /**
     * @return configuration for the server networking.
     */
    protected ServerBootstrap createServerBootstrap() {
        EventLoopGroup incomingConnectionGroup = new NioEventLoopGroup();
        EventLoopGroup incomingMessageGroup = new NioEventLoopGroup();

        // Setup server networking
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(incomingConnectionGroup, incomingMessageGroup)
                       .channel(NioServerSocketChannel.class)
                       .childHandler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           public void initChannel(SocketChannel socketChannel) throws Exception {
                               // Called when a client connection is created

                               // Check banlist and kick banned users
                               if (ipAddressKicked(socketChannel)) return;

                               // Set up the channel pipeline
                               buildChannelPipeline(socketChannel.pipeline(),
                                                    NettyServerNetworking.this.networkConfig,
                                                    NettyServerNetworking.this.serverInfo,
                                                    NettyServerNetworking.this.accountService,
                                                    NettyServerNetworking.this.messageListeners);
                           }
                       })
                       .option(ChannelOption.SO_BACKLOG, 128)
                       .childOption(ChannelOption.SO_KEEPALIVE, true);
        return serverBootstrap;
    }

    /**
     * Builds the pipeline for a connection between a client and the server.
     * @param pipeline pipeline to add handlers to
     * @param networkConfig network configuration.
     * @param serverInfo information about the server.
     * @param accountService service handling login and accounts.
     * @param messageListeners listeners to forward client messages to.
     */
    protected void buildChannelPipeline(ChannelPipeline pipeline,
                                        final NetworkConfig networkConfig,
                                        final ServerInfo serverInfo,
                                        final AccountService accountService,
                                        final List<MessageListener> messageListeners) {
        NettyPipelineBuilder.buildCommonServerHandlers(networkConfig, pipeline);
        pipeline.addLast(new ServerInfoHandler(serverInfo));
        pipeline.addLast(new AuthenticationHandler(accountService));
        pipeline.addLast(new MessageListenerHandler(messageListeners));
    }

    /**
     * @return the server network channel.
     */
    protected Channel getServerChannel() {
        return serverChannel;
    }

    /**
     * @return true if the ip address of the given socketChannel was banned and the connection has been closed.
     */
    private boolean ipAddressKicked(SocketChannel socketChannel) {
        if (banList != null) {
            final InetAddress ipAddress = socketChannel.remoteAddress().getAddress();
            if (banList.isBanned(ipAddress)) {
                log.debug("Kicked banned IP " + ipAddress.toString() + " that tried to connect");
                socketChannel.close();
                return true;
            }
        }

        return false;
    }

}
