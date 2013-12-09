package org.messageduct.server;

import org.apache.mina.filter.firewall.Subnet;
import org.messageduct.account.AccountService;

import java.net.InetAddress;
import java.util.Set;

/**
 * Server side network service.
 *
 * NOTE: ServerNetworking does not manage AccountService - it's up to the caller to make sure the AccountService is initialized and shut down.
 */
public interface ServerNetworking {

    /**
     * @param accountService the accountService used to check users login names and passwords,
     *                      create new users and set their initial passwords,
     *                      or change user passwords.
     */
    void setAccountService(AccountService accountService);

    /**
     * @return the authenticator currently in use.
     */
    AccountService getAccountService();

    /**
     * Adds a listener that is notified about all messages received from connected and logged-in users.
     */
    void addMessageListener(MessageListener listener);

    /**
     * @param listener listener to remove.
     */
    void removeMessageListener(MessageListener listener);

    /**
     * Adds a class that will be allowed to be transferred over connections created in the future.
     * Should not be called after start is called.
     *
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     */
    void registerAllowedClass(Class aClass);

    /**
     * Adds a set of classes that will be allowed to be transferred over connections created in the future.
     * Should not be called after start is called.
     *
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     */
    void registerAllowedClasses(Class... classes);

    /**
     * Adds a set of classes that will be allowed to be transferred over connections created in the future.
     * Should not be called after start is called.
     *
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     */
    void registerAllowedClasses(Set<Class> classes);

    /**
     * @return unmodifiable set of all the allowed classes.
     *         Authentication related classes such as LoginMessage and CreateAccountMessage are not included in this set.
     */
    Set<Class> getAllowedClasses();

    /**
     * Starts listening to the port specified in the constructor, and handling client connections.
     * @throws Exception if there was a problem binding to the port, or some other issue.
     */
    void start() throws Exception;

    /**
     * Unbinds the server from the port and shuts down the thread pool handling client connections.
     */
    void shutdown();

    /**
     * @return true if this ServerNetworking has been started.
     */
    boolean isStarted();

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


    /**
     * @return the port that the server is listening on.
     */
    int getPort();

    /**
     * Can not be called after start has been called.
     *
     * @param port the port that the server should listen on.
     */
    void setPort(int port);

    /**
     * @return number of seconds after an idle event is triggered.
     * Non-logged in users are disconnected after this time,
     * and logged in users get an idle event sent to the MessageListeners.
     */
    int getIdleTimeSeconds();

    /**
     * Can not be called after start has been called.
     *
     * @param idleTimeSeconds number of seconds after an idle event is triggered.
     * Non-logged in users are disconnected after this time,
     * and logged in users get an idle event sent to the MessageListeners.
     */
    void setIdleTimeSeconds(int idleTimeSeconds);
}
