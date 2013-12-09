package org.messageduct.account.messages;

/**
 * Message for requesting a password reset.
 */
public class PasswordResetRequestMessage implements AccountMessage, NonAuthenticatedAccountMessage {
    private final String username;

    public PasswordResetRequestMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
