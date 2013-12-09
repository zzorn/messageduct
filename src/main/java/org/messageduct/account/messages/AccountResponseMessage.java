package org.messageduct.account.messages;

/**
 * Interface for messages that may be returned by an AccountService to the client.
 */
public interface AccountResponseMessage {

    /**
     * @return true if we should close connection to the client after sending this message.
     */
    boolean shouldCloseConnection();

}
