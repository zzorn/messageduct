package org.messageduct.client;

import org.flowutils.Symbol;
import org.messageduct.common.NetworkConfig;
import org.messageduct.utils.service.ServiceBase;

import java.net.InetAddress;
import java.util.Set;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 *
 */
public class MinaClientNetworking extends ServiceBase implements ClientNetworking {

    private final NetworkConfig networkConfig;

    /**
     * @param networkConfig configuration to use for the network connections to servers.
     */
    public MinaClientNetworking(NetworkConfig networkConfig) {
        notNull(networkConfig, "networkConfig");

        this.networkConfig = networkConfig;
    }

    @Override protected void doInit() {
        // TODO: Implement

    }

    @Override protected void doShutdown() {
        // TODO: Implement

    }

    @Override public ServerSession connect(ServerListener listener, ServerInfo serverInfo) {
        ensureActive("connect to server");

        // TODO: Implement
        return null;
    }

    @Override
    public ServerSession login(ServerListener listener, ServerInfo serverInfo, Symbol accountName, char[] password) {
        ensureActive("login to server");

        // TODO: Implement
        return null;
    }

    @Override
    public ServerSession createAccount(ServerListener listener,
                                       ServerInfo serverInfo,
                                       Symbol accountName,
                                       char[] password) {
        ensureActive("create account on server");

        // TODO: Implement
        return null;
    }

}
