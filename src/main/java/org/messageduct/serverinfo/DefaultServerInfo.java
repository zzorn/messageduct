package org.messageduct.serverinfo;

import java.net.InetSocketAddress;
import java.security.PublicKey;

/**
 * Default ServerInfo implementation.
 *
 * Can be overridden with more application specific data if desired.
 */
// TODO: Store public key as serialized in ServerInfo, and store internet address as a string (and port number).
// TODO  This allows serialization of ServerInfo without dragging along a lot of internal implementation classes.
public class DefaultServerInfo implements ServerInfo {

    private InetSocketAddress address;
    private String name;
    private String description;
    private PublicKey publicKey;

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

    /**
     * @param address internet hostname or IP address and port of the server.
     * @param name name of the server.
     * @param description description of the server.
     * @param publicKey public key of the server, used by the client to identify the server and avoid man-in-the-middle attacks.
     */
    public DefaultServerInfo(InetSocketAddress address, String name, String description, PublicKey publicKey) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.publicKey = publicKey;
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

    @Override public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
