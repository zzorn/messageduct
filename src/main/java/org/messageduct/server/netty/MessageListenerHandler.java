package org.messageduct.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.flowutils.LogUtils;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.DeleteAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;
import org.messageduct.server.MessageListener;

import java.util.List;

import static org.flowutils.Check.notNull;

/**
 * Forwards received messages to MessageListeners.
 */
// TODO: Detect idle users?  Maybe measure idletime.
public final class MessageListenerHandler extends ChannelInboundHandlerAdapter {

    private final List<MessageListener> messageListeners;
    private NettyUserSession userSession;

    public MessageListenerHandler(List<MessageListener> messageListeners) {
        notNull(messageListeners, "messageListeners");

        this.messageListeners = messageListeners;
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (userSession != null) {
            userSession.onClosed();

            // Tell listeners that the user disconnected
            for (MessageListener messageListener : messageListeners) {
                messageListener.userDisconnected(userSession);
            }

            userSession = null;
        }
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        // Wait for login or account created message
        if (message instanceof LoginSuccessMessage) {
            // Login ok
            if (userSession == null) {
                userSession = new NettyUserSession(((LoginSuccessMessage) message).getUserName(), ctx.channel());

                for (MessageListener messageListener : messageListeners) {
                    messageListener.userConnected(userSession);
                }

            } else {
                protocolError(ctx, "user already had a session");
            }
        }
        else if (message instanceof CreateAccountSuccessMessage) {
            // Account created
            if (userSession == null) {
                userSession = new NettyUserSession(((CreateAccountSuccessMessage) message).getUserName(), ctx.channel());

                for (MessageListener messageListener : messageListeners) {
                    messageListener.userCreated(userSession);
                }

            } else {
                protocolError(ctx, "user already had a session");
            }
        }
        else if (message instanceof DeleteAccountSuccessMessage) {
            // Account deleted
            if (userSession != null) {
                for (MessageListener messageListener : messageListeners) {
                    messageListener.userDeleted(userSession);
                }

            } else {
                protocolError(ctx, "user did not have a session");
            }
        }
        else {
            // Forward message to listeners
            for (MessageListener messageListener : messageListeners) {
                messageListener.messageReceived(userSession, message);
            }
        }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtils.getLogger().error("Exception caught in server networking, disconnecting: " + cause + ": " + cause.getMessage(), cause);
        ctx.close();
    }

    private void protocolError(ChannelHandlerContext ctx, String msg) {
        LogUtils.getLogger().error("Illegal state: " + msg);
        ctx.close();
    }
}
