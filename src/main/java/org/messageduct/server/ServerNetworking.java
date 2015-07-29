package org.messageduct.server;

import org.apache.mina.filter.firewall.Subnet;
import org.flowutils.service.Service;
import org.messageduct.account.AccountService;
import org.messageduct.utils.banlist.BanList;

import java.net.InetAddress;
import java.util.Set;

/**
 * Server side network service.
 *
 * NOTE: ServerNetworking does not manage AccountService - it's up to the caller to make sure the AccountService is initialized and shut down.
 */
public interface ServerNetworking extends Service {

    /**
     * Adds a listener that is notified about all messages received from connected and logged-in users.
     */
    void addMessageListener(MessageListener listener);

    /**
     * @param listener listener to remove.
     */
    void removeMessageListener(MessageListener listener);

    /**
     * Starts listening to the configured port, and handling client connections.
     */
    void init();

    /**
     * Unbinds the server from the port and shuts down the thread pool handling client connections.
     */
    void shutdown();

    /**
     * @param banList banlist to use to determine if an IP address is allowed to connect to the server.
     */
    void setBanList(BanList banList);

    /**
     * @return banlist to use to determine if an IP address is allowed to connect to the server.
     */
    BanList getBanList();


}
