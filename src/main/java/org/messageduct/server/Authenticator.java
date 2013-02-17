package org.messageduct.server;

import org.messageduct.message.ErrorMessage;

/**
 * Handles account logins and account creation.
 */
public interface Authenticator {

    /**
     * @param accountName name of account to log in to.
     * @param password password for the account.
     * @return null if authentication succeeded, otherwise an error message to be sent to the client.
     */
    ErrorMessage authenticate(String accountName, char[] password);

    /**
     * @param accountName name of account to create.
     * @param password password for the new account.
     * @return null if account creation succeeded, otherwise an error message to be sent to the client.
     */
    ErrorMessage createAccount(String accountName, char[] password);

}
