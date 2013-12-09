package org.messageduct.account;

import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;

import java.util.Set;

/**
 * Handles account logins and account registration, as well as other account related activities.
 *
 * Should support concurrent calls to its methods.
 */
public interface AccountService {

    /**
     * @return the messages that this AccountService handles.
     */
    Set<Class<? extends AccountMessage>> getHandledMessageTypes();

    /**
     * @return other classes that the AccountMessages handled by this AccountService may contain.
     */
    Set<Class> getOtherAcceptedClasses();

    /**
     * Handle the specified AccountMessage.
     *
     * @return AccountResponseMessage to send to the client
     */
    AccountResponseMessage handleMessage(AccountMessage accountMessage);

}
