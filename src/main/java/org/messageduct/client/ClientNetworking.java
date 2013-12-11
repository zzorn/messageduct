package org.messageduct.client;

import org.flowutils.Symbol;
import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.client.serverinfo.ServerInfo;
import org.messageduct.utils.service.Service;

/**
 * Client side networking service.
 */
public interface ClientNetworking extends Service {

    /**
     * Connect to the specified server.
     *
     * @param listener listener that is notified about messages from the server, and connect / disconnect events.
     * @param serverInfo contains the server internet address and port.
     * @return session that can be used to send messages to the server.
     */
    ServerSession connect(ServerListener listener, ServerInfo serverInfo);

    /**
     * Connect to the specified server, and attempt to log in to an existing account.
     *
     * @param listener listener that is notified about messages from the server, and connect / disconnect events.
     * @param serverInfo contains the server internet address and port.
     * @param accountName name of account to log into.
     * @param password password for the account.
     * @return session that can be used to send messages to the server.
     */
    ServerSession login(ServerListener listener, ServerInfo serverInfo, Symbol accountName, char[] password);

    /**
     * Connect to the specified server, and attempt to create a new account.
     *
     * @param listener listener that is notified about messages from the server, and connect / disconnect events.
     * @param serverInfo contains the server internet address and port.
     * @param accountName account name for the new account.
     * @param password password for the new account.
     * @return session that can be used to send messages to the server.
     */
    ServerSession createAccount(ServerListener listener, ServerInfo serverInfo, Symbol accountName, char[] password);

    /**
     * Connect to the specified server, and attempt to create a new account.
     *
     * @param listener listener that is notified about messages from the server, and connect / disconnect events.
     * @param serverInfo contains the server internet address and port.
     * @param accountName account name for the new account.
     * @param password password for the new account.
     * @param email email to use for password recovery and updates.
     * @return session that can be used to send messages to the server.
     */
    ServerSession createAccount(ServerListener listener, ServerInfo serverInfo, Symbol accountName, char[] password, String email);

    /**
     * Connect to the specified server, and attempt to create a new account.
     *
     * @param listener listener that is notified about messages from the server, and connect / disconnect events.
     * @param serverInfo contains the server internet address and port.
     * @param createAccountMessage account creation details.
     * @return session that can be used to send messages to the server.
     */
    ServerSession createAccount(ServerListener listener, ServerInfo serverInfo, CreateAccountMessage createAccountMessage);



}
