package org.messageduct.server;


/**
 * Called when a message is received from a specified logged in user.
 */
public interface MessageListener {

    /**
     * @param session the session object for the connection to the user.
     *                Can be used to get the username, send messages to the user, or close the session.
     * @param message the received message.  Will be one of the registered acceptable message types.
     */
    void messageReceived(UserSession session, Object message);

    /**
     * Called when a new user account is created, authenticated, and connected to the server.
     * userConnected is called after this.
     * @param session the session object for the connection to the new user.
     *                Can be used to get the username, send messages to the user, or close the session.
     */
    void userCreated(UserSession session);

    /**
     * Called when a user account is permanently deleted.
     * @param session the session object for the connection to the user.
     *                Can be used to get the username, send messages to the user, or close the session.
     */
    void userDeleted(UserSession session);

    /**
     * Called when the specified user connected to the server, and authenticated or created an account.
     * If an account was created, userCreated is called first, then userConnected.
     * @param session the session object for the connection to the user.
     *                Can be used to get the username, send messages to the user, or close the session.
     */
    void userConnected(UserSession session);

    /**
     * Called when the specified user disconnected from the server.
     * @param session the session object for the connection to the user.
     *                Can be used to get the username, send messages to the user, or close the session.
     */
    void userDisconnected(UserSession session);

}
