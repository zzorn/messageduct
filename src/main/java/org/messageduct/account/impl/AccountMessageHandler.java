package org.messageduct.account.impl;

import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;
import org.messageduct.account.persistence.AccountPersistence;

/**
 * Handles some type of account message.
 */
public interface AccountMessageHandler<T extends AccountMessage> {

    /**
     * Handle the specified account message.
     *
     * This can be called from multiple threads at once.
     *
     * @return response to send back to the client.  Also indicates success or failure of the action.
     */
    AccountResponseMessage handleMessage(T message);
}
