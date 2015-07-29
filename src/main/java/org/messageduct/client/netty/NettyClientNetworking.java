package org.messageduct.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.flowutils.LogUtils;
import org.messageduct.client.ClientNetworkingBase;
import org.messageduct.common.NetworkConfig;
import org.messageduct.common.netty.NettyPipelineBuilder;
import org.messageduct.serverinfo.ServerInfo;


/**
 * A connection from a client to a server.
 * A client may have several NettyServerSessions active and connected to different (or the same) server.
 */
public class NettyClientNetworking extends ClientNetworkingBase {

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private ChannelFuture channelFuture;
    private Channel channel;

    private final ChannelInboundHandlerAdapter inboundHandler = new ChannelInboundHandlerAdapter() {
        @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // Store channel for future use
            channel = ctx.channel();

            // Notify listeners
            onConnected();
        }

        @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // Notify listeners
            onDisconnected();

            // Channel no longer needed
            channel = null;

            // Shutdown thread pool
            workerGroup.shutdownGracefully();
        }

        @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Notify listeners
            onMessage(msg);
        }

        @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // Notify listeners
            onException(cause);
        }
    };


    @Override protected void doConnect(final NetworkConfig networkConfig, final ServerInfo serverInfo) {
        // Configure client networking
        Bootstrap clientConfig = new Bootstrap();
        clientConfig.group(workerGroup);
        clientConfig.channel(NioSocketChannel.class);
        clientConfig.option(ChannelOption.SO_KEEPALIVE, true);
        clientConfig.option(ChannelOption.AUTO_READ, true);
        clientConfig.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                // Build the handlers used by this client on the channel pipeline
                NettyPipelineBuilder.buildCommonClientHandlers(networkConfig, channel.pipeline(), serverInfo);
                channel.pipeline().addLast(inboundHandler);
            }
        });

        // Start connecting to the server
        channelFuture = clientConfig.connect(serverInfo.getAddress());
    }

    @Override protected void doSendMessage(Object message) {
        if (channel != null) {
            // TODO: Smarter flush handling, maybe only flush every 10 ms or so?
            channel.writeAndFlush(message);
        }
        else {
            final String msg = "Can not send message, no connected channel to server";
            LogUtils.getLogger().warn(msg);
            throw new IllegalStateException(msg);
        }
    }

    @Override protected void doDisconnect() {
        if (channel != null) {
            channel.disconnect();
        }
        else if (channelFuture != null) {
            channelFuture.cancel(true);
        }
    }
}
