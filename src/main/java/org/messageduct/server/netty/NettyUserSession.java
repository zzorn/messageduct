package org.messageduct.server.netty;

import io.netty.channel.Channel;
import org.flowutils.Check;
import org.messageduct.server.UserSession;

import static org.flowutils.Check.notNull;

/**
 *
 */
public final class NettyUserSession implements UserSession {

    private final String userName;
    private Channel channel;

    public NettyUserSession(String userName, Channel channel) {
        Check.nonEmptyString(userName, "userName");
        notNull(channel, "channel");

        this.channel = channel;
        this.userName = userName;
    }

    @Override public String getUserName() {
        return userName;
    }

    @Override public void sendMessage(Object message) {
        if (channel != null) {
            // TODO: Intelligent flush handling, flush if not flushed for some milliseconds.
            channel.writeAndFlush(message);
        }
    }

    @Override public void disconnectUser() {
        if (channel != null) {
            channel.close();
        }
    }

    public void onClosed() {
        channel = null;
    }
}
