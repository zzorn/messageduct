package org.messageduct.account;

import org.flowutils.service.Service;
import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;


/**
 * Handles account logins and account registration, as well as other account related activities.
 *
 * Should support concurrent calls to its methods.
 */
public interface AccountService extends Service {

    /**
     * Handle the specified AccountMessage.
     *
     * @return AccountResponseMessage to send to the client, or null to send no response
     */
    AccountResponseMessage handleMessage(AccountMessage accountMessage);

    /**
     * Creates a new account with the specified username and password.
     * Provided as an alternative to sending an AccountCreationMessage, for use e.g. in unit tests.
     * @param userName username to create an account for.
     * @param password the password to use.  This field will not be scrubbed.
     */
    void createAccount(String userName, char[] password);
}
