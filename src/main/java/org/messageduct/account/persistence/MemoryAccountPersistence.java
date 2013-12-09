package org.messageduct.account.persistence;

import org.messageduct.account.model.Account;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory only implementation of AccountPersistence.
 */
public final class MemoryAccountPersistence implements AccountPersistence {

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

    @Override public void init() {
    }

    @Override public void shutdown() {
        accounts.clear();
    }

    @Override public Account getAccount(String userName) {
        return accounts.get(userName);
    }

    @Override public boolean hasAccount(String userName) {
        return accounts.containsKey(userName);
    }

    @Override public boolean createAccount(String userName, Account account) {
        final Account oldAccount = accounts.putIfAbsent(userName, account);
        return oldAccount == null; // Success if there was no account with the specified userName.
    }

    @Override public boolean updateAccount(String userName, Account account) {
        final Account oldValue = accounts.replace(userName, account);
        return oldValue != null; // Only succeeded if there was an account with that name
    }

    @Override public boolean deleteAccount(String userName) {
        final Account oldValue = accounts.remove(userName);
        return oldValue != null;
    }

    /**
     * @return a read only view of the currently registered accounts.
     */
    public Map<String, Account> getAccounts() {
        return Collections.unmodifiableMap(accounts);
    }
}
