package org.messageduct.account.persistence;

import org.messageduct.account.model.Account;
import org.messageduct.utils.service.ServiceBase;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory only implementation of AccountPersistence.
 */
public final class MemoryAccountPersistence extends ServiceBase implements AccountPersistence {

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

    @Override public Account getAccount(String userName) {
        ensureActive();
        return accounts.get(userName);
    }

    @Override public boolean hasAccount(String userName) {
        ensureActive();
        return accounts.containsKey(userName);
    }

    @Override public boolean createAccount(String userName, Account account) {
        ensureActive();
        final Account oldAccount = accounts.putIfAbsent(userName, account);
        return oldAccount == null; // Success if there was no account with the specified userName.
    }

    @Override public boolean updateAccount(String userName, Account account) {
        ensureActive();
        final Account oldValue = accounts.replace(userName, account);
        return oldValue != null; // Only succeeded if there was an account with that name
    }

    @Override public boolean deleteAccount(String userName) {
        ensureActive();
        final Account oldValue = accounts.remove(userName);
        return oldValue != null;
    }

    /**
     * @return a read only view of the currently registered accounts.
     */
    public Map<String, Account> getAccounts() {
        ensureActive();
        return Collections.unmodifiableMap(accounts);
    }
}
