package org.messageduct.account.messages;

/**
 * Message used by a client for authentication to a server.
 */
public class LoginMessage extends AccountMessageBase implements NonAuthenticatedAccountMessage {

    public LoginMessage(String username, char[] password) {
        super(username, password);
    }
}
