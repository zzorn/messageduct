package org.messageduct.client;

import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.common.NetworkConfig;
import org.messageduct.serverinfo.ServerInfo;

import java.net.InetSocketAddress;

/**
 * Represents a connection to a server.
 * It supports one lifecycle only, connect, communication, and disconnect.
 * After disconnect it can no longer reconnect, a new ServerSession is needed for that.
 */
public interface ClientNetworking {

    /**
     * Starts connecting to the specified server using the specified network configuration parameters.
     * Any listeners are notified when the connection is completed, or if there is some problem when connecting.
     *
     * @param networkConfig network configuration to use.
     * @param hostname hostname of server to connect to.
     * @param port port of server to connect to.
     */
    void connect(NetworkConfig networkConfig, String hostname, int port);

    /**
     * Starts connecting to the specified server using the specified network configuration parameters.
     * Any listeners are notified when the connection is completed, or if there is some problem when connecting.
     *
     * @param networkConfig network configuration to use.
     * @param serverAddress server address to connect to.
     */
    void connect(NetworkConfig networkConfig, InetSocketAddress serverAddress);

    /**
     * Starts connecting to the specified server using the specified network configuration parameters.
     * Any listeners are notified when the connection is completed, or if there is some problem when connecting.
     *
     * @param networkConfig network configuration to use.
     * @param serverInfo information about the server we are going to connect to.
     */
    void connect(NetworkConfig networkConfig, ServerInfo serverInfo);

    /**
     * Disconnect from the server if not already disconnected.
     */
    void disconnect();

    /**
     * Initiates a login handshake.
     * Connect must have been called first, although the connection does not yet need to be ready
     * (the login is queued if the connection is still ongoing).
     *
     * @param accountName account name.
     * @param password password for the account.
     */
    void login(String accountName, char[] password);

    /**
     * Initiates an account creation request.
     * Connect must have been called first, although the connection does not yet need to be ready
     * (the account creation message is queued if the connection is still ongoing).
     *
     * @param accountName desired account name.
     * @param password password for the account.
     */
    void createAccount(String accountName, char[] password);

    /**
     * Initiates an account creation request.
     * Connect must have been called first, although the connection does not yet need to be ready
     * (the account creation message is queued if the connection is still ongoing).
     *
     * @param accountName desired account name.
     * @param password password for the account.
     * @param email email to use for password recovery and updates.
     */
    void createAccount(String accountName, char[] password, String email);

    /**
     * Initiates an account creation request.
     * Connect must have been called first, although the connection does not yet need to be ready
     * (the account creation message is queued if the connection is still ongoing).
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
     * @return true if we got disconnected from the server, or if we disconnected from it ourselves.
     * After disconnection this ServerSession can no longer be used.
     */
    boolean isDisconnected();

    /**
     * @return the account we are logged in as, or trying to log in as, or null if not yet specified.
     */
    String getAccountName();


}
