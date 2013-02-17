package org.messageduct.message;

/**
 * Message sent by the client to create a new account on a server.
 */
public class CreateAccountMessage implements Message {
    private final String username;
    private final char[] password;

    public CreateAccountMessage(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }
}
