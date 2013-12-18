package org.messageduct.server;

/**
 * Adapter for MessageListener.
 *
 * Override the methods you are interested in.
 */
public abstract class MessageListenerAdapter implements MessageListener {

    @Override public void messageReceived(UserSession session, Object message) {
    }

    @Override public void userCreated(UserSession session) {
    }

    @Override public void userDeleted(UserSession session) {
    }

    @Override public void userConnected(UserSession session) {
    }

    @Override public void userDisconnected(UserSession session) {
    }

    @Override public void userIdle(UserSession session) {
    }
}
