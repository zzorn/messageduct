package org.messageduct.message;

/**
 * Message used by a client for authentication to a server.
 */
public class LoginMessage implements Message {
    private final String username;
    private final char[] password;

    public LoginMessage(String username, char[] password) {
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
