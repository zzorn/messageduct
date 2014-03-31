package org.messageduct.client.mina;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.Symbol;
import org.flowutils.service.ServiceBase;
import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.serverinfo.ServerInfo;
import org.messageduct.client.ServerListener;
import org.messageduct.client.ServerSession;
import org.messageduct.common.NetworkConfig;

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

    @Override public ServerSession connect(ServerInfo serverInfo, ServerListener listener) {
        ensureActive("connect to server");

        final ServerSession session = createSession(serverInfo);
        session.addListener(listener);
        session.connect();

        return session;
    }

    @Override
    public ServerSession login(ServerInfo serverInfo,
                               String accountName, char[] password, ServerListener listener) {
        ensureActive("login to server");

        System.out.println("MinaClientNetworking.login");

        final ServerSession session = createSession(serverInfo);
        session.addListener(listener);
        session.login(accountName, password);

        return session;
    }

    @Override
    public ServerSession createAccount(ServerInfo serverInfo,
                                       String accountName,
                                       char[] password,
                                       ServerListener listener) {
        return createAccount(serverInfo, accountName, password, null, listener);
    }

    @Override
    public ServerSession createAccount(ServerInfo serverInfo,
                                       String accountName,
                                       char[] password,
                                       String email, ServerListener listener) {
        return createAccount(serverInfo, new CreateAccountMessage(accountName, password, email), listener);
    }

    @Override
    public ServerSession createAccount(ServerInfo serverInfo,
                                       CreateAccountMessage createAccountMessage,
                                       ServerListener listener) {
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
