package org.messageduct.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.flowutils.LogUtils;
import org.messageduct.account.messages.*;
import org.messageduct.serverinfo.ServerInfo;
import org.messageduct.serverinfo.ServerInfoMessage;
import org.messageduct.serverinfo.ServerInfoRequestMessage;

import static org.flowutils.Check.notNull;

/**
 * Pipeline handler that responds to ServerInfo requests, and may send server info to clients when they connect.
 */
public final class ServerInfoHandler extends ChannelInboundHandlerAdapter {

    private final ServerInfo serverInfo;
    private final boolean sendServerInfoOnConnect;

    /**
     * @param serverInfo information about this server (name, description, public key, etc).
     */
    public ServerInfoHandler(ServerInfo serverInfo) {
        this(serverInfo, true);
    }

    /**
     * @param serverInfo information about this server (name, description, public key, etc).
     * @param sendServerInfoOnConnect if true automatically sends the server info to the clients when they connect.
     */
    public ServerInfoHandler(ServerInfo serverInfo, boolean sendServerInfoOnConnect) {
        notNull(serverInfo, "serverInfo");

        this.sendServerInfoOnConnect = sendServerInfoOnConnect;
        this.serverInfo = serverInfo;
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // Channel connected, send server info to client if configured that way
        if (sendServerInfoOnConnect) {
            sendServerInfo(ctx);
        }

        // Pass on the activation event to other handlers
        super.channelActive(ctx);
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {

        // Handle server info requests
        if (message instanceof ServerInfoRequestMessage) {
            sendServerInfo(ctx);
        }
        else {
            // Not a server info request, pass message on
            super.channelRead(ctx, message);
        }
    }

    private void sendServerInfo(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new ServerInfoMessage(serverInfo));
    }

}
