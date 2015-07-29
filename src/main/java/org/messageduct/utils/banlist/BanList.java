package org.messageduct.utils.banlist;

import java.net.InetAddress;

/**
 * Handles banned IPs or IP ranges.
 */
public interface BanList {

    /**
     * @param ipAddress internet address to test.
     * @return true if the specified IP is banned.
     */
    boolean isBanned(InetAddress ipAddress);

    /**
     * @param ipAddress IP address to permanently ban.
     */
    void banIp(InetAddress ipAddress);

    /**
     * @param ipAddress IP address to ban.
     * @param banDurationMilliseconds how long the ban is, in milliseconds.  If zero, the ban is permanent (or until unbanned).
     */
    void banIp(InetAddress ipAddress, long banDurationMilliseconds);

    /**
     * Removes the specified IP address from the banlist, allowing connections from it again.
     * @param ipAddress IP address to unban.
     */
    void unBanIp(InetAddress ipAddress);
}
