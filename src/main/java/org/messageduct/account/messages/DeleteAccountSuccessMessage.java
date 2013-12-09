package org.messageduct.account.messages;

/**
 *
 */
public class DeleteAccountSuccessMessage extends AccountResponseMessageBase  {

    public DeleteAccountSuccessMessage(String userName) {
        super(userName);
    }
}
