package org.messageduct.client;

import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.ErrorMessage;
import org.messageduct.account.messages.LoginSuccessMessage;

/**
 * Listens to events from the server
 */
public interface ServerListener {

    /**
     * Called when a message is received from the server.
     *
     * @param serverSession the session to the server.  Can be used for replies etc.
     * @param message received object.
     */
    void onMessage(ServerSession serverSession, Object message);

    /**
     * Called when a connection is established to the server, but not necessarily yet logged in.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onConnected(ServerSession serverSession);

    /**
     * Called when a connection is disconnected to the server.
     *
     * @param serverSession the session to the server.  Now disconnected.
     */
    void onDisconnected(ServerSession serverSession);

    /**
     * Called when no message has been sent or received for some time.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onIdle(ServerSession serverSession);

    /**
     * Called when log in was successful.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onLoggedIn(ServerSession serverSession, LoginSuccessMessage loginSuccessMessage);

    /**
     * Called when account creation was successful.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onAccountCreated(ServerSession serverSession, CreateAccountSuccessMessage createAccountSuccessMessage);

    /**
     * Called when there was some error message from the server.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onErrorMessage(ServerSession serverSession, ErrorMessage errorMessage);

    /**
     * Called when there was some communication error or the like.
     *
     * @param serverSession the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onError(ServerSession serverSession, Exception e);


}
