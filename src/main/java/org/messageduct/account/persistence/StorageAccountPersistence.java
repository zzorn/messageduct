package org.messageduct.account.persistence;

import org.flowutils.service.ServiceProvider;
import org.messageduct.account.model.Account;
import org.messageduct.utils.storage.FileStorage;
import org.messageduct.utils.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account persistence that saves the accounts to a storage provider whenever any account is changed.
 *
 * Note that this stores all accounts when any account changes, which could be ok for a medium number of accounts and a file storage,
 * but not suitable for a large number of accounts and a database storage.
 */
public final class StorageAccountPersistence extends MemoryAccountPersistence {

    private final Storage storage;

    /**
     * Creates a new FileStorage backed AccountPersistence with no encryption.
     *
     * @param storageFile file to store the accounts in.
     */
    public StorageAccountPersistence(File storageFile) {
        this(storageFile, null);
    }

    /**
     * Creates a new FileStorage backed AccountPersistence.
     *
     * @param storageFile file to store the accounts in.
     * @param accountFilePassword a password to use to encrypt the accounts with, or null to use no encryption.
     */
    public StorageAccountPersistence(File storageFile, char[] accountFilePassword) {
        this(new FileStorage(storageFile, accountFilePassword));
    }

    /**
     * Creates a new Storage backed AccountPersistence.
     *
     * @param storage storage to use to save and load accounts with.
     */
    public StorageAccountPersistence(Storage storage) {
        this.storage = storage;
    }


    @Override protected void doInit(ServiceProvider serviceProvider) {
        // Load accounts
        final Map<String, Account> storedAccounts;
        try {
            storedAccounts = storage.load();
        } catch (IOException e) {
            throw new IllegalStateException("Problem when loading stored accounts: " + e.getMessage(), e);
        }

        // On first startup stored accounts will be null
        if (storedAccounts != null) {
            // Update accounts
            setAccounts(storedAccounts);
        }
    }

    @Override protected void doShutdown() {
        super.doShutdown();
    }

    @Override protected void store(ConcurrentHashMap <String, Account> accounts) {
        // Put data in HashMap for easier serialization(?)
        final HashMap<String, Account> data = new HashMap<String, Account>(accounts);

        // Save
        try {
            storage.save(data);
        } catch (IOException e) {
            throw new IllegalStateException("Problem when saving account data: "+ e.getMessage(), e);
        }
    }
}
