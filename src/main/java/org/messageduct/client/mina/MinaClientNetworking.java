package org.messageduct.client.mina;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.Symbol;
import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.serverinfo.ServerInfo;
import org.messageduct.client.ServerListener;
import org.messageduct.client.ServerSession;
import org.messageduct.common.NetworkConfig;
import org.messageduct.utils.service.ServiceBase;

import java.util.Set;

import static org.flowutils.Check.notNull;

/**
 * ClientNetworking implementation that uses the Mina network library.
 */
public class MinaClientNetworking extends ServiceBase implements ClientNetworking {

    private final NetworkConfig networkConfig;
    private final Set<ServerSession> serverSessions = new ConcurrentHashSet<ServerSession>();

    /**
     * @param networkConfig configuration to use for the network connections to servers.
     */
    public MinaClientNetworking(NetworkConfig networkConfig) {
        notNull(networkConfig, "networkConfig");

        this.networkConfig = networkConfig;
    }

    @Override protected void doInit() {
    }

    @Override protected void doShutdown() {
        // Disconnect all opened sessions
        for (ServerSession serverSession : serverSessions) {
            serverSession.disconnect();
        }
    }

    @Override public ServerSession connect(ServerListener listener, ServerInfo serverInfo) {
        ensureActive("connect to server");

        final ServerSession session = createSession(serverInfo);
        session.addListener(listener);
        session.connect();

        return session;
    }

    @Override
    public ServerSession login(ServerListener listener, ServerInfo serverInfo, Symbol accountName, char[] password) {
        ensureActive("login to server");

        final ServerSession session = createSession(serverInfo);
        session.addListener(listener);
        session.login(accountName, password);

        return session;
    }

    @Override
    public ServerSession createAccount(ServerListener listener,
                                       ServerInfo serverInfo,
                                       Symbol accountName,
                                       char[] password) {
        return createAccount(listener, serverInfo, accountName, password, null);
    }

    @Override
    public ServerSession createAccount(ServerListener listener,
                                       ServerInfo serverInfo,
                                       Symbol accountName,
                                       char[] password,
                                       String email) {
        return createAccount(listener, serverInfo, new CreateAccountMessage(accountName.getName(), password, email));
    }

    @Override
    public ServerSession createAccount(ServerListener listener,
                                       ServerInfo serverInfo,
                                       CreateAccountMessage createAccountMessage) {
        ensureActive("create account on server");

        final ServerSession session = createSession(serverInfo);
        session.addListener(listener);
        session.createAccount(createAccountMessage);

        return session;
    }

    private ServerSession createSession(ServerInfo serverInfo) {
        final MinaServerSession session = new MinaServerSession(serverInfo, networkConfig);
        serverSessions.add(session);
        return session;
    }

}
