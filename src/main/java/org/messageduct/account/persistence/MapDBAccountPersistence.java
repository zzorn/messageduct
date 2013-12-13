package org.messageduct.account.persistence;

import org.flowutils.Check;
import org.flowutils.Symbol;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.messageduct.account.model.Account;
import org.messageduct.account.model.DefaultAccount;
import org.messageduct.utils.serializer.ConcurrentSerializer;
import org.messageduct.utils.serializer.ConcurrentSerializerWrapper;
import org.messageduct.utils.serializer.KryoSerializer;
import org.messageduct.utils.service.ServiceBase;

import java.io.*;

import static org.flowutils.Check.notNull;

/**
 * AccountPersistence implemented with MapDB.
 *
 * @deprecated Hard to use custom serializer.  Will be removed.
 */
public final class MapDBAccountPersistence extends ServiceBase implements AccountPersistence {

    public static final String ACCOUNTS_TABLE_NAME = "Accounts";

    private final ConcurrentSerializer concurrentSerializer;
    private final MapDBSerializerAdapter mapDBSerializerAdapter;

    private final DB db;
    private HTreeMap<String, Account> accounts;

    /**
     * Connects to a non-encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     */
    public MapDBAccountPersistence(File databaseFile) {
        this(databaseFile, createDefaultSerializer());
    }

    /**
     * Connects to a non-encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     * @param concurrentSerializer serializer to use to store the account data
     */
    public MapDBAccountPersistence(File databaseFile, ConcurrentSerializer concurrentSerializer) {
        this(DBMaker.newFileDB(databaseFile).
                randomAccessFileEnableIfNeeded().
                       checksumEnable().
                       closeOnJvmShutdown().
                       compressionEnable().
                       make(), concurrentSerializer
        );
    }

    /**
     * Connects to an encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     * @param accountDatabasePassword password used to encrypt the account database file with.
     */
    public MapDBAccountPersistence(File databaseFile, String accountDatabasePassword) {
        this(databaseFile, accountDatabasePassword, createDefaultSerializer());
    }

    /**
     * Connects to an encrypted account database.
     *
     * @param databaseFile file that the account database is stored in or should be stored in.
     * @param accountDatabasePassword password used to encrypt the account database file with.
     * @param concurrentSerializer serializer to use to store the account data
     */
    public MapDBAccountPersistence(File databaseFile, String accountDatabasePassword, ConcurrentSerializer concurrentSerializer) {
        this(DBMaker.newFileDB(databaseFile).
                randomAccessFileEnableIfNeeded().
                       checksumEnable().
                       closeOnJvmShutdown().
                       compressionEnable().
                       encryptionEnable(accountDatabasePassword).
                       make(),
             concurrentSerializer);
    }

    /**
     * Connects to the specified account database.
     *
     * @param db database to use.
     */
    public MapDBAccountPersistence(DB db) {
        this(db, createDefaultSerializer());
    }

    /**
     * Connects to the specified account database.
     *
     * @param db database to use.
     * @param concurrentSerializer serializer to use to store the account data
     */
    public MapDBAccountPersistence(DB db, ConcurrentSerializer concurrentSerializer) {
        notNull(concurrentSerializer, "concurrentSerializer");
        notNull(db, "db");

        this.concurrentSerializer = concurrentSerializer;
        this.db = db;

        mapDBSerializerAdapter = new MapDBSerializerAdapter(concurrentSerializer);
    }


    @Override protected void doInit() {
        // Create table if necessary
        if (!db.exists(ACCOUNTS_TABLE_NAME)) {

            // Create map with custom serializer
            db.createHashMap(ACCOUNTS_TABLE_NAME).valueSerializer(mapDBSerializerAdapter).make();

            // Save created map
            db.commit();
        }

        // Open accounts map
        accounts = db.getHashMap(ACCOUNTS_TABLE_NAME);
    }

    @Override protected void doShutdown() {
        // Close database
        db.close();
    }

    @Override public Account getAccount(String userName) {
        ensureActive();
        Check.nonEmptyString(userName, "userName");

        return accounts.get(userName);
    }

    @Override public boolean hasAccount(String userName) {
        ensureActive();
        Check.nonEmptyString(userName, "userName");

        return accounts.containsKey(userName);
    }

    @Override public boolean createAccount(String userName, Account account) {
        ensureActive();
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
        ensureActive();
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
        ensureActive();
        Check.nonEmptyString(userName, "userName");

        final Account oldValue = accounts.remove(userName);

        // Succeeded if there was an account with that name
        if (oldValue != null) {
            db.commit(); // Commit changes made to the map
            return true;
        }
        else return false;
    }

    /**
     * Serializer adapter to use our own serializer for storage serialization.
     */
    private static class MapDBSerializerAdapter implements Serializer, Serializable {
        private final ConcurrentSerializer delegateSerializer;

        private MapDBSerializerAdapter(ConcurrentSerializer delegateSerializer) {
            this.delegateSerializer = delegateSerializer;
        }

        @Override public void serialize(DataOutput out, Object value) throws IOException {
            out.write(delegateSerializer.serialize(value));
        }

        @Override public Object deserialize(final DataInput in, int available) throws IOException {
            return delegateSerializer.deserialize(new InputStream() {
                @Override public int read() throws IOException {
                    try {
                        return in.readUnsignedByte();
                    }
                    catch (EOFException e) {
                        return -1;
                    }
                }
            });
        }
    }

    private static ConcurrentSerializerWrapper createDefaultSerializer() {
        final ConcurrentSerializerWrapper serializer = new ConcurrentSerializerWrapper(KryoSerializer.class);

        serializer.registerAllowedClass(DefaultAccount.class);
        serializer.registerAllowedClass(Symbol.class);

        return serializer;
    }


}
