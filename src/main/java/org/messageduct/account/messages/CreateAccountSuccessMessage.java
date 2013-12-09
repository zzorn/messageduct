package org.messageduct.account.messages;

/**
 *
 */
public class CreateAccountSuccessMessage extends AccountResponseMessageBase  {

    public CreateAccountSuccessMessage(String userName) {
        super(userName);
    }
}
