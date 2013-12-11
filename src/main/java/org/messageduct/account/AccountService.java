package org.messageduct.account;

import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;


/**
 * Handles account logins and account registration, as well as other account related activities.
 *
 * Should support concurrent calls to its methods.
 */
public interface AccountService {

    /**
     * Handle the specified AccountMessage.
     *
     * @return AccountResponseMessage to send to the client, or null to send no response
     */
    AccountResponseMessage handleMessage(AccountMessage accountMessage);

}
