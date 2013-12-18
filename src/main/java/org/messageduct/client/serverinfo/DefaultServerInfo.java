package org.messageduct.client.serverinfo;

import java.net.InetSocketAddress;

/**
 * Default ServerInfo implementation.
 *
 * Can be overridden with more application specific data if desired.
 */
public class DefaultServerInfo implements ServerInfo {

    private InetSocketAddress address;
    private String name;
    private String description;

    /**
     * @param hostname hostname to connect to
     * @param port port to connect to
     */
    public DefaultServerInfo(String hostname, int port) {
        this(new InetSocketAddress(hostname, port));
    }

    /**
     * @param address internet hostname or IP address and port of the server.
     */
    public DefaultServerInfo(InetSocketAddress address) {
        this(address, address.getHostString(), null);
    }

    /**
     * @param address internet hostname or IP address and port of the server.
     * @param name name of the server.
     */
    public DefaultServerInfo(InetSocketAddress address, String name) {
        this(address, name, null);
    }

    /**
     * @param address internet hostname or IP address and port of the server.
     * @param name name of the server.
     * @param description description of the server.
     */
    public DefaultServerInfo(InetSocketAddress address, String name, String description) {
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    @Override public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @Override public void setDescription(String description) {
        this.description = description;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    @Override public void setAddress(InetSocketAddress address) {
        this.address = address;
    }
}
