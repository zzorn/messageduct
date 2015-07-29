package org.messageduct.client;

import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;

/**
 * Listens to events from the server or connection to the server.
 */
public interface ServerListener {

    /**
     * Called when a message is received from the server.
     *
     * @param clientNetworking the session to the server.  Can be used for replies etc.
     * @param message received object.
     */
    void onMessage(ClientNetworking clientNetworking, Object message);

    /**
     * Called when a connection is established to the server, but not necessarily yet logged in.
     *
     * @param clientNetworking the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onConnected(ClientNetworking clientNetworking);

    /**
     * Called when a connection is disconnected from the server.
     *
     * @param clientNetworking the session to the server.  Now disconnected.
     */
    void onDisconnected(ClientNetworking clientNetworking);

    /**
     * Called when log in was successful.
     *
     * @param clientNetworking the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onLoggedIn(ClientNetworking clientNetworking, LoginSuccessMessage loginSuccessMessage);

    /**
     * Called when account creation was successful.
     *
     * @param clientNetworking the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onAccountCreated(ClientNetworking clientNetworking, CreateAccountSuccessMessage createAccountSuccessMessage);

    /**
     * Called when there was some account related error message from the server.
     *
     * @param clientNetworking the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onAccountErrorMessage(ClientNetworking clientNetworking, AccountErrorMessage accountErrorMessage);

    /**
     * Called when there was some communication error or the like.
     *
     * @param clientNetworking the session to the server.  Can be used for sending messages, disconnecting, etc.
     */
    void onException(ClientNetworking clientNetworking, Throwable e);


}
