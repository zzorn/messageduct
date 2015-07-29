package org.messageduct.serverinfo;

import static org.flowutils.Check.notNull;

/**
 * Message from server to client with the servers ServerInfo.
 */
public final class ServerInfoMessage {
    private final ServerInfo serverInfo;

    public ServerInfoMessage(ServerInfo serverInfo) {
        notNull(serverInfo, "serverInfo");

        this.serverInfo = serverInfo;
    }

    /**
     * @return information about the server.
     */
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
