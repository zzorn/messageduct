package org.messageduct.client;

/**
 * Represents a connection to a server.
 */
public interface ServerSession {

    void addListener(ServerListener listener);
    void removeListener(ServerListener listener);

    void sendMessage(Object message);

    void disconnect();
}
