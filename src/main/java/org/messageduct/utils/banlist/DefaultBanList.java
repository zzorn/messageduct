package org.messageduct.utils.banlist;

import org.flowutils.LogUtils;
import org.flowutils.serializer.KryoSerializer;
import org.messageduct.utils.storage.FileStorage;
import org.messageduct.utils.storage.Storage;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.flowutils.Check.notNull;

/**
 * Simple banlist implementation that allows banning IPs for some duration or permanently, and unbanning them.
 * Thread safe.
 * Periodically saves banned IPs to the supplied storage or banlist file.
 */
public final class DefaultBanList implements BanList {

    private static final long MIN_SAVE_INTERVAL_MILLISECONDS = 59*1000L;

    private final ConcurrentMap<InetAddress, Long> bannedIPs = new ConcurrentHashMap<InetAddress, Long>();
    private final Storage storage;
    private final AtomicBoolean banListUpdated = new AtomicBoolean(false);

    /**
     * @param banlistFile banlist file to load and save banlist to.
     */
    public DefaultBanList(File banlistFile) {
        this(new FileStorage(banlistFile,
                             null,
                             // Only allow serialization of what we need to store:
                             new KryoSerializer(
                                     Long.class,
                                     byte[].class,
                                     HashMap.class,
                                     HashMap.SimpleEntry.class,
                                     HashMap.SimpleImmutableEntry.class
                             ),
                             null),
             null);
    }

    /**
     * @param storage storage to load and save banlist to.
     */
    public DefaultBanList(Storage storage) {
        this(storage, null);
    }

    /**
     * @param storage storage to load and save banlist to.
     * @param banDurations initial bans not in storage.
     */
    public DefaultBanList(Storage storage, Map<InetAddress, Long> banDurations) {
        notNull(storage, "storage");

        this.storage = storage;

        // Load stored bans
        reloadFromStorage();

        // Init bans
        if (banDurations != null) {
            bannedIPs.putAll(banDurations);
            banListUpdated.set(true);

            // Save the current bans to storage.
            saveToStorageIfNeeded();
        }

        // Start a thread to save the banlist at some interval if it has been updated
        if (storage != null) {
            Thread storageUpdateThread = new Thread(new Runnable() {
                @Override public void run() {
                    while(true) {
                        // Delay for some time
                        try {
                            Thread.sleep(MIN_SAVE_INTERVAL_MILLISECONDS);
                        } catch (InterruptedException e) {
                            // Ignore
                        }

                        // Save banlist if needed
                        saveToStorageIfNeeded();
                    }
                }
            });
            storageUpdateThread.setDaemon(true);
            storageUpdateThread.start();
        }

    }

    @Override public boolean isBanned(InetAddress ipAddress) {
        final Long banEndTime = bannedIPs.get(ipAddress);
        if (banEndTime == null) return false;
        else {
            if (banEndTime > 0 && banEndTime <= System.currentTimeMillis()) {
                // The ban has ended, lift it
                bannedIPs.remove(ipAddress);
                banListUpdated.set(true);
                return false;
            }
            else {
                // IP is still banned.
                return true;
            }
        }
    }

    @Override public void banIp(InetAddress ipAddress) {
        banIp(ipAddress, 0);
    }

    @Override public void banIp(InetAddress ipAddress, long banDurationMilliseconds) {
        if (banDurationMilliseconds == 0) {
            // Permanently banned
            bannedIPs.put(ipAddress, 0L);
            banListUpdated.set(true);
        }
        else {
            // Banned for some time - but if it was earlier banned for a longer time, don't update
            long banEndTime = System.currentTimeMillis() + banDurationMilliseconds;
            final Long oldBanEndTime = bannedIPs.get(ipAddress);
            if (oldBanEndTime == null || oldBanEndTime < banEndTime) {
                // Only replace ban if it is longer than the old ban
                bannedIPs.put(ipAddress, banEndTime);
                banListUpdated.set(true);
            }
        }

        saveToStorageIfNeeded();
    }

    @Override public void unBanIp(InetAddress ipAddress) {
        bannedIPs.remove(ipAddress);
    }

    /**
     * Reloads the banlist from the storage specified in the constructor.
     * Existing bans are replaced with the ones currently in storage.
     */
    public void reloadFromStorage() {
        // Clear old bans
        bannedIPs.clear();

        try {
            // Load bans
            Map<byte[], Long> bans = storage.load();

            // Convert bans from byte representation to InetAddress objects and store them in memory
            if (bans != null) {
                for (Map.Entry<byte[], Long> entry : bans.entrySet()) {
                    bannedIPs.put(InetAddress.getByAddress(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Could not load banlist: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the banlist to the storage specified in the constructor, if the banlist has been changed.
     */
    public void saveToStorageIfNeeded() {
        if (banListUpdated.getAndSet(false)) {
            Map<byte[], Long> bans = new HashMap<byte[], Long>();
            for (Map.Entry<InetAddress, Long> entry : bannedIPs.entrySet()) {
                bans.put(entry.getKey().getAddress(), entry.getValue());
            }

            try {
                storage.save(bans);
            } catch (Exception e) {
                LogUtils.getLogger().error("Could not save banlist: " + e.getMessage(), e);
            }
        }
    }
}
