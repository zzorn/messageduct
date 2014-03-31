package org.messageduct.client;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.Check;
import org.flowutils.ThreadUtils;
import org.messageduct.account.messages.*;
import org.messageduct.client.serverinfo.ServerInfo;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.flowutils.Check.notNull;

/**
 * Common functionality for ServerSessions.
 */
public abstract class ServerSessionBase implements ServerSession {

    private final Set<ServerListener> listeners = new ConcurrentHashSet<ServerListener>();
    private final Deque<Object> queuedMessages = new ConcurrentLinkedDeque<Object>();

    private final ServerInfo serverInfo;
    private String accountName = null;

    private boolean connectCalled = false;
    private boolean disconnectCalled = false;
    private boolean gotDisconnected = false;
    private boolean connected = false;
    private boolean loggedIn = false;

    /**
     * @param serverInfo information about the server we are going to connect to.  May be updated with new information from the server.
     */
    protected ServerSessionBase(ServerInfo serverInfo) {
        notNull(serverInfo, "serverInfo");

        this.serverInfo = serverInfo;
    }

    @Override public final ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override public final void connect() {
        ensureNotDisconnected();
        if (connectCalled) throw new IllegalStateException("Connect was already called");

        connectCalled = true;
        doConnect();
    }

    @Override public final void disconnect() {
        if (!disconnectCalled) {
            disconnectCalled = true;
            doDisconnect();
        }
    }

    @Override public final void login(String accountName, char[] password) {

        System.out.println("ServerSessionBase.login");

        ensureNotDisconnected();
        if (loggedIn) throw new IllegalStateException("Already logged in with account name " + this.accountName +", can not log in with account '" + accountName +"'");
        this.accountName = accountName;

        // Send login message
        sendMessage(new LoginMessage(accountName, password));

        // Call connect if not already done
        if (!connectCalled) connect();
    }

    @Override public final void createAccount(String accountName, char[] password) {
        ensureNotDisconnected();
        createAccount(new CreateAccountMessage(accountName, password));
    }

    @Override public final void createAccount(String accountName, char[] password, String email) {
        ensureNotDisconnected();
        createAccount(new CreateAccountMessage(accountName, password, email));
    }

    @Override public final void createAccount(CreateAccountMessage createAccountMessage) {
        ensureNotDisconnected();
        if (loggedIn) throw new IllegalStateException("Already logged in with account name " + this.accountName +", can not create a new account named '" + accountName + "'");
        this.accountName = createAccountMessage.getUsername();

        // Send account creation
        sendMessage(createAccountMessage);

        // Call connect if not already done
        if (!connectCalled) connect();
    }

    @Override public final void sendMessage(Object message) {
        Check.notNull(message, "message");

        System.out.println("ServerSessionBase.sendMessage " + message);

        // Queue messages if we are not connected
        if (!isConnected()) {
            System.out.println("   queued"  );
            queuedMessages.add(message);
        }
        else {
            System.out.println("   sent"  );
            doSendMessage(message);
        }
    }

    @Override public final String getAccountName() {
        return accountName;
    }

    @Override public final boolean isLoggedIn() {
        return loggedIn;
    }

    @Override public final boolean isConnected() {
        return connected;
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
            // Normal message
            for (ServerListener listener : listeners) {
                listener.onMessage(this, message);
            }
        }
    }

    /**
     * Call when a connection is established to the server, but not necessarily yet logged in.
     */
    protected final void onConnected() {
        System.out.println("ServerSessionBase.onConnected");
        connected = true;

        for (ServerListener listener : listeners) {
            listener.onConnected(this);
        }

        // Send queued messages
        System.out.println("Sending queued messages: " + queuedMessages.size());
        Object message = queuedMessages.poll();
        while (message != null) {
            System.out.println("   message = " + message);
            doSendMessage(message);
            message = queuedMessages.poll();
        }
    }

    /**
     * Call when a connection is disconnected from the server.
     */
    protected final void onDisconnected() {
        connected = false;
        loggedIn = false;
        gotDisconnected = true;

        for (ServerListener listener : listeners) {
            listener.onDisconnected(this);
        }
    }

    /**
     * Call when no message has been sent or received for some time.
     */
    protected final void onIdle() {
        for (ServerListener listener : listeners) {
            listener.onIdle(this);
        }
    }

    /**
     * Call when log in was successful.
     */
    private void onLoggedIn(LoginSuccessMessage loginSuccessMessage) {
        loggedIn = true;

        for (ServerListener listener : listeners) {
            listener.onLoggedIn(this, loginSuccessMessage);
        }
    }

    /**
     * Call when account creation was successful.
     */
    private void onAccountCreated(CreateAccountSuccessMessage createAccountSuccessMessage) {
        loggedIn = true;

        for (ServerListener listener : listeners) {
            listener.onAccountCreated(this, createAccountSuccessMessage);
        }
    }

    /**
     * Call when there was some error message from the server.
     */
    private void onAccountErrorMessage(AccountErrorMessage accountErrorMessage) {
        for (ServerListener listener : listeners) {
            listener.onAccountErrorMessage(this, accountErrorMessage);
        }
    }

    /**
     * Call when there was some communication error or the like.
     */
    protected final void onException(Throwable e) {
        // TODO: DEBUG, remove
        System.out.println("ServerSessionBase.onException");
        System.out.println("e = " + e);
        e.printStackTrace();


        for (ServerListener listener : listeners) {
            listener.onException(this, e);
        }
    }


    /**
     * Connect to the server.
     */
    protected abstract void doConnect();

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
