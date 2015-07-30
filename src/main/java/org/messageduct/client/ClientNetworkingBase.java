package org.messageduct.client;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.LogUtils;
import org.flowutils.ThreadUtils;
import org.messageduct.account.messages.*;
import org.messageduct.common.NetworkConfig;
import org.messageduct.serverinfo.DefaultServerInfo;
import org.messageduct.serverinfo.ServerInfo;
import org.messageduct.serverinfo.ServerInfoMessage;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.flowutils.Check.notNull;

/**
 * Common functionality for ClientNetworking.
 */
public abstract class ClientNetworkingBase implements ClientNetworking {

    private final Set<ServerListener> listeners = new ConcurrentHashSet<ServerListener>();
    private final Deque<Object> queuedMessages = new ConcurrentLinkedDeque<Object>();
    private final Logger log = LogUtils.getLogger();

    private ServerInfo serverInfo;
    private String accountName = null;

    private boolean connectCalled = false;
    private boolean disconnectCalled = false;
    private boolean gotDisconnected = false;
    private boolean connected = false;
    private boolean loggedIn = false;

    @Override public final void connect(NetworkConfig networkConfig, String hostname, int port) {
        connect(networkConfig, new DefaultServerInfo(hostname, port));
    }

    @Override public final void connect(NetworkConfig networkConfig, InetSocketAddress serverAddress) {
        connect(networkConfig, new DefaultServerInfo(serverAddress));
    }

    @Override public final void connect(NetworkConfig networkConfig, ServerInfo serverInfo) {
        notNull(networkConfig, "networkConfig");
        notNull(serverInfo, "serverInfo");
        ensureNotDisconnected();
        if (connectCalled) throw new IllegalStateException("Connect was already called");

        log.info("Connecting to server " + serverInfo.getAddress());
        connectCalled = true;
        doConnect(networkConfig, serverInfo);
    }

    @Override public final void disconnect() {
        if (!disconnectCalled) {
            log.info("Disconnecting");
            disconnectCalled = true;
            doDisconnect();
        }
    }

    @Override public final void login(String accountName, char[] password) {
        ensureNotDisconnected();
        if (loggedIn) throw new IllegalStateException("Already logged in with account name " + this.accountName +", can not log in with account '" + accountName +"'");
        if (!connectCalled) throw new IllegalStateException("connect should be called before login!");

        // Store account name
        this.accountName = accountName;

        // Send (or queue) login message
        log.info("Logging into account '" + accountName + "'");
        sendMessage(new LoginMessage(accountName, password));
    }

    @Override public final void createAccount(String accountName, char[] password) {
        createAccount(new CreateAccountMessage(accountName, password));
    }

    @Override public final void createAccount(String accountName, char[] password, String email) {
        createAccount(new CreateAccountMessage(accountName, password, email));
    }

    @Override public final void createAccount(CreateAccountMessage createAccountMessage) {
        ensureNotDisconnected();
        if (loggedIn) throw new IllegalStateException("Already logged in with account name " + this.accountName +", can not create a new account named '" + accountName + "'");
        if (!connectCalled) throw new IllegalStateException("connect should be called before createAccount!");

        // Store account name
        this.accountName = createAccountMessage.getUsername();

        // Send (or queue) account creation message
        log.info("Creating new account '" + accountName + "'");
        sendMessage(createAccountMessage);
    }

    @Override public final void sendMessage(Object message) {

        notNull(message, "message");
        if (!connectCalled) throw new IllegalStateException("connect should be called before sendMessage!");
        if (isDisconnected()) {
            log.debug("Disconnected, so ignoring message.");
            return;
        }

        // Queue messages if we are not connected
        if (!isConnected()) {
            queuedMessages.add(message);
        }
        else {
            doSendMessage(message);
        }
    }

    @Override public final String getAccountName() {
        return accountName;
    }

    @Override public final ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override public final boolean isLoggedIn() {
        return loggedIn;
    }

    @Override public final boolean isConnected() {
        return connected;
    }

    @Override public final boolean isDisconnected() {
        return disconnectCalled || gotDisconnected;
    }

    @Override public final void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    @Override public final void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }


    /**
     * Call when a message is received from the server.
     *
     * @param message received object.
     */
    protected final void onMessage(Object message) {
        // Check the received message for account related messages
        if (message instanceof LoginSuccessMessage) {
            onLoggedIn((LoginSuccessMessage) message);
        }
        else if (message instanceof CreateAccountSuccessMessage) {
            onAccountCreated((CreateAccountSuccessMessage) message);
        }
        else if (message instanceof AccountErrorMessage) {
            onAccountErrorMessage((AccountErrorMessage) message);
        }
        else {
            // Handle ServerInfoMessage
            if (message instanceof ServerInfoMessage) {
                // Store server info
                final ServerInfo serverInfoFromServer = ((ServerInfoMessage) message).getServerInfo();
                // TODO: Check that server info signature and public key matches with server public key, if we know it.
                if (serverInfoFromServer != null) {
                    serverInfo = serverInfoFromServer;
                }
            }

            // Forward normal message to listeners
            for (ServerListener listener : listeners) {
                listener.onMessage(this, message);
            }
        }
    }

    /**
     * Call when a connection is established to the server, but not necessarily yet logged in.
     */
    protected final void onConnected() {
        log.info("Connected to server");

        connected = true;

        // Send queued messages
        log.debug("Sending " + queuedMessages.size() + " queued messages");
        Object message = queuedMessages.poll();
        while (message != null) {
            doSendMessage(message);
            message = queuedMessages.poll();
        }

        // Notify listeners
        for (ServerListener listener : listeners) {
            listener.onConnected(this);
        }
    }

    /**
     * Call when a connection is disconnected from the server.
     */
    protected final void onDisconnected() {
        connected = false;
        loggedIn = false;
        gotDisconnected = true;

        log.info("Disconnected from server");

        for (ServerListener listener : listeners) {
            listener.onDisconnected(this);
        }
    }

    /**
     * Call when log in was successful.
     */
    private void onLoggedIn(LoginSuccessMessage loginSuccessMessage) {
        loggedIn = true;

        log.info("Logged into account '" + accountName + "'");

        for (ServerListener listener : listeners) {
            listener.onLoggedIn(this, loginSuccessMessage);
        }
    }

    /**
     * Call when account creation was successful.
     */
    private void onAccountCreated(CreateAccountSuccessMessage createAccountSuccessMessage) {
        loggedIn = true;

        log.info("Account created '" + accountName + "'");

        for (ServerListener listener : listeners) {
            listener.onAccountCreated(this, createAccountSuccessMessage);
        }
    }

    /**
     * Call when there was some error message from the server.
     */
    private void onAccountErrorMessage(AccountErrorMessage accountErrorMessage) {
        log.info("Received account error message: " + accountErrorMessage);

        for (ServerListener listener : listeners) {
            listener.onAccountErrorMessage(this, accountErrorMessage);
        }
    }

    /**
     * Call when there was some communication error or the like.
     */
    protected final void onException(Throwable e) {
        log.error("Network problem: " + e + ": " + e.getMessage(), e);

        for (ServerListener listener : listeners) {
            listener.onException(this, e);
        }
    }


    /**
     * Connect to the server.
     * @param networkConfig network and connection configuration to use.
     * @param serverInfo information about the server to connect to, including the address and port (and public key if known).
     */
    protected abstract void doConnect(NetworkConfig networkConfig, ServerInfo serverInfo);

    /**
     * Send the specified message to the server.
     */
    protected abstract void doSendMessage(Object message);

    /**
     * Disconnect from the server.
     */
    protected abstract void doDisconnect();


    private void ensureNotDisconnected() {
        if (disconnectCalled) throw new IllegalStateException("Can not invoke " + ThreadUtils.getCallingMethodName() + ", disconnect has already been called");
        if (gotDisconnected) throw new IllegalStateException("Can not invoke " + ThreadUtils.getCallingMethodName() + ", was disconnected");
    }
}
