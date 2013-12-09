package org.messageduct.account.persistence;

import org.flowutils.Check;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.messageduct.account.model.Account;

import java.io.File;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 * AccountPersistence implemented with MapDB.
 */
public final class MapDBAccountPersistence implements AccountPersistence {

    public static final String ACCOUNTS_TABLE_NAME = "Accounts";

    private final DB db;
    private HTreeMap<String, Account> accounts;
    private boolean initialized = false;
    private boolean shutdown = false;

    /**
     * Connects to a non-encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     */
    public MapDBAccountPersistence(File databaseFile) {
        this(DBMaker.newFileDB(databaseFile).
                     randomAccessFileEnableIfNeeded().
                     checksumEnable().
                     closeOnJvmShutdown().
                     compressionEnable().
                     make());
    }

    /**
     * Connects to an encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     * @param accountDatabasePassword password used to encrypt the account database file with.
     */
    public MapDBAccountPersistence(File databaseFile, String accountDatabasePassword) {
        this(DBMaker.newFileDB(databaseFile).
                     randomAccessFileEnableIfNeeded().
                     checksumEnable().
                     closeOnJvmShutdown().
                     compressionEnable().
                     encryptionEnable(accountDatabasePassword).
                     make());
    }

    /**
     * Connects to the specified account database.
     *
     * @param db database to use.
     */
    public MapDBAccountPersistence(DB db) {
        notNull(db, "db");

        this.db = db;
    }

    @Override public void init() {
        if (initialized) throw new IllegalStateException("Already initialized");
        initialized = true;

        // Open accounts map
        accounts = db.getHashMap(ACCOUNTS_TABLE_NAME);
    }

    @Override public void shutdown() {
        shutdown = true;
        db.close();
    }

    @Override public Account getAccount(String userName) {
        checkInitialized();
        Check.nonEmptyString(userName, "userName");

        return accounts.get(userName);
    }

    @Override public boolean hasAccount(String userName) {
        checkInitialized();
        Check.nonEmptyString(userName, "userName");

        return accounts.containsKey(userName);
    }

    @Override public boolean createAccount(String userName, Account account) {
        checkInitialized();
        Check.nonEmptyString(userName, "userName");
        Check.notNull(account, "account");

        final Account oldAccount = accounts.putIfAbsent(userName, account);

        // Success if there was no account with the specified userName.
        if (oldAccount == null) {
            db.commit(); // Commit changes made to the map
            return true;
        }
        else return false;
    }

    @Override public boolean updateAccount(String userName, Account account) {
        checkInitialized();
        Check.nonEmptyString(userName, "userName");
        Check.notNull(account, "account");

        final Account oldValue = accounts.replace(userName, account);

        // Only succeeded if there was an account with that name
        if (oldValue != null) {
            db.commit(); // Commit changes made to the map
            return true;
        }
        else return false;
    }

    @Override public boolean deleteAccount(String userName) {
        checkInitialized();
        Check.nonEmptyString(userName, "userName");

        final Account oldValue = accounts.remove(userName);

        // Succeeded if there was an account with that name
        if (oldValue != null) {
            db.commit(); // Commit changes made to the map
            return true;
        }
        else return false;
    }

    private void checkInitialized() {
        if (!initialized) throw new IllegalStateException(this.getClass().getSimpleName() + " is not yet initialized!  Call init before calling other methods!");
        if (shutdown) throw new IllegalStateException(this.getClass().getSimpleName() + " is already shot down!  Can not call any methods after shutdown has been called!");
    }
}
