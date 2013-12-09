package org.messageduct.server;

/**
 * Represents a connection to a user.
 */
public interface UserSession {

    /**
     * @return username of user, as authenticated by the authenticator.
     */
    String getUserName();

    /**
     * Send the specified message to the user.
     * @param message message to send, must contain only white-listed classes.
     */
    void sendMessage(Object message);

    /**
     * Closes the session and disconnects the user
     */
    void disconnectUser();

}
