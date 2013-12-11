package org.messageduct.client;

import org.flowutils.Symbol;

import java.net.InetAddress;

/**
 * Information about a server stored on a client.
 */
public interface ServerInfo {

    /**
     * @return unique identifier used to refer to the server.
     */
    Symbol getIdentifier();

    /**
     * @return user readable name of the server.
     */
    String getName();

    /**
     * @return description of the server.
     */
    String getDescription();

    /**
     * @return internet address of the server.
     */
    InetAddress getAddress();

    /**
     * @return port to connect to on the server, or -1 to use the default port for this application.
     */
    int getPort();


}
