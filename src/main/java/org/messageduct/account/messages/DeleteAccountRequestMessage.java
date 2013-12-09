package org.messageduct.account.messages;

/**
 * Message for requesting an account deletion.
 */
public class DeleteAccountRequestMessage extends AccountMessageBase {

    public DeleteAccountRequestMessage(String username, char[] password) {
        super(username, password);
    }
}
