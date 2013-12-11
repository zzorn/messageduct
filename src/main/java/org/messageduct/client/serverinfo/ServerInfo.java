package org.messageduct.client.serverinfo;

import java.net.InetSocketAddress;

/**
 * Information about a server stored on a client.
 */
public interface ServerInfo {

    /**
     * @return user readable name of server, as reported by server.
     */
    String getName();

    /**
     * @param name user readable name of server, as reported by server.
     */
    void setName(String name);

    /**
     * @return description of the server, as reported by the server.
     */
    String getDescription();

    /**
     * @param description description of the server, as reported by the server.
     */
    void setDescription(String description);

    /**
     * @return internet address or hostname of the server and the port to connect to.
     */
    InetSocketAddress getAddress();

    /**
     * @param address internet address or hostname of the server and the port to connect to.
     */
    void setAddress(InetSocketAddress address);
}
