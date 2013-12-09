package org.messageduct.account.messages;

/**
 * Base class for account related messages sent between server and client.
 */
public interface AccountMessage {

    /**
     * @return the account name that this message is related to.
     *         If the user is logged in, and this account name does not match the logged in user,
     *         the message will be ignored and the connection terminated.
     */
    String getUsername();

}
