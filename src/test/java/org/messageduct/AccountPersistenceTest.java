package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.account.model.Account;
import org.messageduct.account.model.DefaultAccount;
import org.messageduct.account.persistence.AccountPersistence;
import org.messageduct.account.persistence.MemoryAccountPersistence;
import org.messageduct.account.persistence.StorageAccountPersistence;

import java.io.File;

public class AccountPersistenceTest {

    private static final File STORAGE_FILE = new File("AccountPersistenceTestDatabase.db");

    @Test
    public void testBasicOperations() throws Exception {
        checkBasicOperations(new MemoryAccountPersistence());
        checkBasicOperations(createFileBackedAccountPersistence());
    }

    private StorageAccountPersistence createFileBackedAccountPersistence() {
        return new StorageAccountPersistence(STORAGE_FILE, "foobar".toCharArray());
    }

    @Test
    public void testPersistence() throws Exception {
        final String username = "foobar";

        // Delete any old test file
        STORAGE_FILE.delete();

        // Create persistence
        final AccountPersistence accountPersistence = createFileBackedAccountPersistence();
        accountPersistence.init();

        // Delete user account if it existed (e.g. from failed run)
        accountPersistence.deleteAccount(username);
        assertFalse("No account for " + username + " should exist at the moment", accountPersistence.hasAccount(username));

        // Create account and close
        assertTrue("Creation should succeed",
                   accountPersistence.createAccount(username, createTestAccount(username, "foomail")));
        accountPersistence.shutdown();

        // Reload
        final AccountPersistence accountPersistence2 = createFileBackedAccountPersistence();
        accountPersistence2.init();

        // Should remember
        final Account account = accountPersistence2.getAccount(username);
        assertAccountEquals("Should be able to retrieve saved account", username, "foomail", account);

        // Cleanup
        assertTrue(accountPersistence2.deleteAccount(username));
        accountPersistence2.shutdown();

        // Delete test file
        STORAGE_FILE.delete();
    }

    private void checkBasicOperations(AccountPersistence accountPersistence) {
        final String userName = "igor";

        accountPersistence.init();

        // Empty at start
        assertNull("Should be empty initially", accountPersistence.getAccount(userName));
        assertFalse("Should be empty initially", accountPersistence.hasAccount(userName));

        // Create acc
        assertTrue("Account creation should succeed",
                   accountPersistence.createAccount(userName, createTestAccount(userName, userName)));

        // Creating with same username should fail
        assertFalse("Account creation with same username should fail",
                    accountPersistence.createAccount(userName, createTestAccount(userName, userName)));

        // Get it
        final Account account = accountPersistence.getAccount(userName);
        assertNotNull("We should get the account now", account);
        assertTrue("Has account should work", accountPersistence.hasAccount(userName));
        assertAccountEquals("Account should have been stored correctly", userName, userName, account);

        // Update
        accountPersistence.updateAccount(userName, createTestAccount(userName, "newEmail"));
        final Account account2 = accountPersistence.getAccount(userName);
        assertNotNull("We should get the account", account2);
        assertAccountEquals("Account should have been updated correctly", userName, "newEmail", account2);

        // Delete
        assertTrue(accountPersistence.deleteAccount(userName));
        assertNull("Should be gone after delete", accountPersistence.getAccount(userName));
        assertFalse("Should be gone after delete", accountPersistence.hasAccount(userName));

        // Re-Delete should fail
        assertFalse("Should not be able to delete nonexistent account", accountPersistence.deleteAccount(userName));

        accountPersistence.shutdown();
    }

    private void assertAccountEquals(String msg, String expectedUserName, String expectedEmail, Account account) {
        final DefaultAccount expectedAccount = createTestAccount(expectedUserName, expectedEmail);
        assertEquals(msg, expectedAccount.getUserName(), account.getUserName());
        assertEquals(msg, expectedAccount.getEmail(), account.getEmail());
    }

    private DefaultAccount createTestAccount(String userName, String email) {
        return new DefaultAccount(userName, "fooHash", email + "@example.com");
    }

}
