package org.messageduct.client;

import org.flowutils.Symbol;
import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.client.serverinfo.ServerInfo;

/**
 * Represents a connection to a server.
 */
public interface ServerSession {

    /**
     * Connect to the server.
     */
    void connect();

    /**
     * Disconnect from the server if not already disconnected.
     */
    void disconnect();

    /**
     * Initiates a login handshake.
     * Calls connect if it has not already been called.
     *
     * @param accountName account name.
     * @param password password for the account.
     */
    void login(Symbol accountName, char[] password);

    /**
     * Initiates an account creation request.
     * Calls connect if it has not already been called.
     *
     * @param accountName desired account name.
     * @param password password for the account.
     */
    void createAccount(Symbol accountName, char[] password);

    /**
     * Initiates an account creation request.
     * Calls connect if it has not already been called.
     *
     * @param accountName desired account name.
     * @param password password for the account.
     * @param email email to use for password recovery and updates.
     */
    void createAccount(Symbol accountName, char[] password, String email);

    /**
     * Initiates an account creation request.
     * Calls connect if it has not already been called.
     *
     * @param createAccountMessage account creation details.
     */
    void createAccount(CreateAccountMessage createAccountMessage);

    /**
     * Send a message to the server.  Queues the message if we are not yet connected.
     * @param message message to send.
     */
    void sendMessage(Object message);

    /**
     * @param listener listener that gets notified about messages from the server.
     */
    void addListener(ServerListener listener);

    /**
     * @param listener listener to remove.
     */
    void removeListener(ServerListener listener);

    /**
     * @return information about the server we are connected to.
     */
    ServerInfo getServerInfo();

    /**
     * @return true if we are connected to the server (but not necessarily yet logged in).
     */
    boolean isConnected();

    /**
     * @return true if we are connected to the server and logged in to an account.
     */
    boolean isLoggedIn();

    /**
     * @return the account we are logged in as, or trying to log in as, or null if not yet specified.
     */
    Symbol getAccountName();


}
