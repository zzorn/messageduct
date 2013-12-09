package org.messageduct.server.minaserver;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.DeleteAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;
import org.messageduct.server.MessageListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listens to messages from connected users and forwards them to application provided message listeners.
 */
public final class ConnectionHandler extends IoHandlerAdapter implements IoHandler {

    private final CopyOnWriteArrayList<MessageListener> messageListeners = new CopyOnWriteArrayList<MessageListener>();

    public ConnectionHandler() {
    }

    /**
     * @param listener a listener that will be notified about all received messages.
     */
    public ConnectionHandler(MessageListener listener) {
        messageListeners.add(listener);
    }

    /**
     * @param listeners listeners that will be notified about all received messages.
     */
    public ConnectionHandler(List<MessageListener> listeners) {
        messageListeners.addAll(listeners);
    }

    /**
     * @param listener a listener that will be notified about all received messages.
     */
    public void addListener(MessageListener listener) {
        messageListeners.addIfAbsent(listener);
    }

    /**
     * @param listener listener to remove
     */
    public void removeListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        // Get UserSession of user if logged in
        final MinaUserSession userSession = AuthenticationFilter.getUserSession(session);

        // Notify application if the user was logged in
        if (userSession != null) {
            for (MessageListener messageListener : messageListeners) {
                messageListener.userDisconnected(userSession);
            }
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        // Get UserSession of user if logged in
        final MinaUserSession userSession = AuthenticationFilter.getUserSession(session);

        if (userSession == null) {
            // If the user is not logged in, disconnect them when they idle
            session.close(true);
        }
        else {
            // If logged in let the application decide what to do
            for (MessageListener messageListener : messageListeners) {
                messageListener.userIdle(userSession);
            }
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        // Get UserSession of user if logged in
        final MinaUserSession userSession = AuthenticationFilter.getUserSession(session);

        // TODO: log
        System.out.println("Exception in session " + session +", user session:  "+userSession+": " + cause.getMessage() + cause);
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        // Get UserSession of user if logged in
        final MinaUserSession userSession = AuthenticationFilter.getUserSession(session);
        if (userSession == null) {
            // We should be logged in and have a username if we are here.  If not, panic:
            session.close(true);
            throw new IllegalStateException("No username found in session " + session);
        }

        // Handle the message
        if (message == null) {
            // Null messages are not acceptable
            session.close(true);
            // TODO: Log error
            System.out.println("message was null in session " + session);
        }
        else if (message instanceof LoginSuccessMessage) {
            // User logged in
            for (MessageListener messageListener : messageListeners) {
                messageListener.userConnected(userSession);
            }
        }
        else if (message instanceof CreateAccountSuccessMessage) {
            // A new account was created
            for (MessageListener messageListener : messageListeners) {
                messageListener.userCreated(userSession);
            }
        }
        else if (message instanceof DeleteAccountSuccessMessage) {
            // An account was deleted
            for (MessageListener messageListener : messageListeners) {
                messageListener.userDeleted(userSession);
            }
        }
        else {
            // Normal message
            for (MessageListener messageListener : messageListeners) {
                messageListener.messageReceived(userSession, message);
            }
        }
    }

}
