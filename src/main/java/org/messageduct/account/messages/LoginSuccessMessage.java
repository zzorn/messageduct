package org.messageduct.account.messages;

/**
 *
 */
public class LoginSuccessMessage extends AccountResponseMessageBase  {

    public LoginSuccessMessage(String userName) {
        super(userName);
    }
}
