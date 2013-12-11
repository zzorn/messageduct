package org.messageduct.server;

import org.apache.mina.filter.firewall.Subnet;
import org.messageduct.account.AccountService;
import org.messageduct.utils.service.Service;

import java.net.InetAddress;
import java.util.Set;

/**
 * Server side network service.
 *
 * NOTE: ServerNetworking does not manage AccountService - it's up to the caller to make sure the AccountService is initialized and shut down.
 */
// TODO: Separate banlist into own class? (For easier save & load, as well as more complicated logic, e.g. timed or permanent bans)
// TODO: Add support for a generic server info message that is sent when connected but not logged in, that has a server name & description and motd, and where application can subclass it to include relevant metadata.
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
     * Blacklists a specific ip.
     * A banned ip will be immediately disconnected if it tries to connect.
     */
    void banIp(InetAddress address);

    /**
     * Removes blacklisting of a previously blacklisted ip.
     * (If the subnet that the ip is in is blacklisted, the ip will still be blacklisted).
     */
    void unBanIp(InetAddress address);

    /**
     * Blacklists a whole subnet.
     * An ip from a banned subnet will be immediately disconnected if it tries to connect.
     */
    void banSubnet(Subnet subnet);

    /**
     * Removes blacklisting of a previously blacklisted subnet.
     * If there are other blacklistings affecting parts of the subnet, they are not removed.
     */
    void unBanSubnet(Subnet subnet);

}
