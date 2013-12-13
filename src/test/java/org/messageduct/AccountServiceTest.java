package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.messageduct.account.AccountService;
import org.messageduct.account.DefaultAccountService;
import org.messageduct.account.messages.*;
import org.messageduct.account.persistence.MemoryAccountPersistence;

public class AccountServiceTest {

    private MemoryAccountPersistence accountPersistence;
    private AccountService accountService;
    private char[] password;
    private String username;
    private String email;

    @Before
    public void setUp() throws Exception {
        accountPersistence = new MemoryAccountPersistence();
        accountService = new DefaultAccountService(accountPersistence);

        password = "veryPasswordSoWow".toCharArray();
        username = "doge";
        email    = "muchEmail@example.com";
    }

    @Test
    public void testSunnyDay() throws Exception {
        // Need to init first
        accountService.init();

        // Should not be added yet
        assertFalse(accountPersistence.hasAccount(username));

        // Test create account
        checkCreateAccount(true, false, username, password, email);
        assertTrue(accountPersistence.hasAccount(username));
        assertEquals(username, accountPersistence.getAccount(username).getUserName());
        assertEquals(email, accountPersistence.getAccount(username).getEmail());

        // Test login
        checkLogin(true, false, username, password);

        // Done
        accountService.shutdown();
    }

    @Test
    public void testInvalidCreationPassword() throws Exception {
        // Need to init first
        accountService.init();

        // Should not be added yet
        assertFalse(accountPersistence.hasAccount(username));

        // Test create account
        checkCreateAccount(false, false, username, "soWeak".toCharArray(), email);
        assertFalse(accountPersistence.hasAccount(username));
    }


    @Test
    public void testInvalidCreationUsername() throws Exception {
        // Need to init first
        accountService.init();

        // Should not be added yet
        assertFalse(accountPersistence.hasAccount(username));

        // Test create account
        checkCreateAccount(false, false, "   so space  ", password, email);
        assertFalse(accountPersistence.hasAccount(username));
    }

    @Test
    public void testAccountAlreadyExists() throws Exception {
        // Need to init first
        accountService.init();

        // Should not be added yet
        assertFalse(accountPersistence.hasAccount(username));

        // Create account
        checkCreateAccount(true, false, username, password, email);
        assertEquals(username, accountPersistence.getAccount(username).getUserName());
        assertEquals(email, accountPersistence.getAccount(username).getEmail());

        // Try creating account with same name
        checkCreateAccount(false, false, username, password, "soOther@example.com");

        // Should not have changed data
        assertEquals(username, accountPersistence.getAccount(username).getUserName());
        assertEquals(email, accountPersistence.getAccount(username).getEmail());
    }


    @Test
    public void testLoginWithWrongPassword() throws Exception {
        // Need to init first
        accountService.init();

        // Create account
        checkCreateAccount(true, false, username, password, email);
        assertTrue(accountPersistence.hasAccount(username));

        // Try login
        checkLogin(false, true, username, "soWrongPassword".toCharArray());
    }

    @Test
    public void testLoginWithNonExistingUser() throws Exception {
        // Need to init first
        accountService.init();

        // Create account
        checkCreateAccount(true, false, username, password, email);
        assertTrue(accountPersistence.hasAccount(username));

        // Try login
        checkLogin(false, false, "soStrangeUser", password);
    }


    @Test
    public void testNoInit() throws Exception {

        // Test create account
        try {
            checkCreateAccount(false, false, username, password, email);
            fail("Should require initialization");
        }
        catch (IllegalStateException e) {
            // Ok
        }
    }

    @Test
    public void testNoInitAccountPersistenceCheck() throws Exception {

        // Test create account
        try {
            accountPersistence.hasAccount(username);
            fail("Should require initialization");
        }
        catch (IllegalStateException e) {
            // Ok
        }
    }

    @Test
    public void testAfterShutdown() throws Exception {

        accountService.init();
        accountService.shutdown();

        // Test create account
        try {
            checkCreateAccount(false, false, username, password, email);
            fail("Should fail after shutdown");
        }
        catch (IllegalStateException e) {
            // Ok
        }
    }


    private AccountResponseMessage checkCreateAccount(boolean expectedSuccess,
                                                      boolean shouldCloseConnection,
                                                      final String username,
                                                      final char[] password,
                                                      final String email) {
        final CreateAccountMessage accountMessage = new CreateAccountMessage(username, password, email);
        final AccountResponseMessage responseMessage = accountService.handleMessage(accountMessage);

        if (expectedSuccess) {
            assertTrue("Should have succeeded", CreateAccountSuccessMessage.class.isInstance(responseMessage));
            assertEquals("Username should be same", username, ((CreateAccountSuccessMessage)responseMessage).getUserName());
        }
        else {
            assertTrue("Should have failed", AccountErrorMessage.class.isInstance(responseMessage));
        }

        assertEquals("Connection closing should be correct", shouldCloseConnection, responseMessage.shouldCloseConnection());

        return responseMessage;
    }

    private AccountResponseMessage checkLogin(boolean expectedSuccess,
                                              boolean shouldCloseConnection,
                                              final String username,
                                              final char[] password) {
        final AccountResponseMessage responseMessage = accountService.handleMessage(new LoginMessage(username, password));
        if (expectedSuccess) {
            assertTrue("Should have succeeded", LoginSuccessMessage.class.isInstance(responseMessage));
            assertEquals("Username should be same", username, ((LoginSuccessMessage)responseMessage).getUserName());
        }
        else {
            assertTrue("Should have failed", AccountErrorMessage.class.isInstance(responseMessage));
        }

        assertEquals("Connection closing should be correct", shouldCloseConnection, responseMessage.shouldCloseConnection());
        return responseMessage;
    }



}
