package org.messageduct.account.persistence;

import org.messageduct.account.model.Account;
import org.messageduct.utils.service.ServiceBase;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory only implementation of AccountPersistence.
 *
 * Can be overridden to provide persistent storage when the data changes.
 */
public class MemoryAccountPersistence extends ServiceBase implements AccountPersistence {

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<String, Account>();

    /**
     * Create a new MemoryAccountPersistence with no initial accounts
     */
    public MemoryAccountPersistence() {
        this(null);
    }

    /**
     * Create a new MemoryAccountPersistence with the specified initial accounts
     * @param initialAccounts initial accounts to add, or null if none should be added.
     */
    public MemoryAccountPersistence(Map<String, Account> initialAccounts) {
        if (initialAccounts != null) {
            accounts.putAll(initialAccounts);
        }
    }

    @Override protected void doInit() {
        // Nothing to do
    }

    @Override protected void doShutdown() {
        accounts.clear();
    }

    @Override public final Account getAccount(String userName) {
        ensureActive();
        return accounts.get(userName);
    }

    @Override public final boolean hasAccount(String userName) {
        ensureActive();
        return accounts.containsKey(userName);
    }

    @Override public final boolean createAccount(String userName, Account account) {
        ensureActive();

        // Attempt to claim the userName
        final Account oldAccount = accounts.putIfAbsent(userName, account);

        // Success if there was no account with the specified userName.
        final boolean success = oldAccount == null;

        // Notify child classes that we could commit changes now
        if (success) store(accounts);

        return success;
    }


    @Override public final boolean updateAccount(String userName, Account account) {
        ensureActive();

        // Attempt to replace with new version
        final Account oldValue = accounts.replace(userName, account);

        // Only succeeded if there was an account with that name
        final boolean success = oldValue != null;

        // Notify child classes that we could commit changes now
        if (success) store(accounts);

        return success;
    }

    @Override public final boolean deleteAccount(String userName) {
        ensureActive();

        // Attempt to delete account
        final Account oldValue = accounts.remove(userName);

        // Success if there was some account with that name
        final boolean success = oldValue != null;

        // Notify child classes that we could commit changes now
        if (success) store(accounts);

        return success;
    }

    /**
     * @return a read only view of the currently registered accounts.
     */
    public final Map<String, Account> getAccounts() {
        ensureActive();
        return Collections.unmodifiableMap(accounts);
    }

    /**
     * Called when the data should be stored.  Does nothing in this class, subclasses may override.
     *
     * @param accounts accounts to store
     */
    protected void store(ConcurrentHashMap<String, Account> accounts) {
    }

    /**
     * Can be called by a storage to specify the current accounts
     *
     * Not thread safe, other threads should not simultaneously call account modifying operations.
     *
     * @param accounts accounts to replace current ones with.
     */
    protected final void setAccounts(Map<String, Account> accounts) {
        this.accounts.clear();
        this.accounts.putAll(accounts);
    }

}
