package org.messageduct.account.messages;

import org.messageduct.utils.SecurityUtils;

/**
 * Base class for account messages that contain a user name and password.
 */
public abstract class AccountMessageBase implements AccountMessage {
    private final String username;
    private final char[] password;

    protected AccountMessageBase(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public final String getUsername() {
        return username;
    }

    public final char[] getPassword() {
        return password;
    }

    /**
     * Erases the password from memory.
     */
    public void scrubPassword() {
        SecurityUtils.scrubChars(password);
    }

    @Override protected void finalize() throws Throwable {
        scrubPassword();
        super.finalize();
    }
}
