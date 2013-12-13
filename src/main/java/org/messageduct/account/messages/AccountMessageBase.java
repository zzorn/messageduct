package org.messageduct.account.messages;

import org.messageduct.utils.SecurityUtils;

import java.util.Arrays;

/**
 * Base class for account messages that contain a user name and password.
 */
public abstract class AccountMessageBase implements AccountMessage {
    private final String username;
    private final char[] password;

    /**
     * @param username username.
     * @param password password to use, a copy of this array will be made,
     *                 so if desired the array can be scrubbed (with SecurityUtils.scrubChars)
     *                 after passed in if it is not needed anymore.
     */
    protected AccountMessageBase(String username, char[] password) {
        this.username = username;

        // Get a copy of the password chars, the caller is responsible of scrubbing the password passed in if desired.
        this.password = Arrays.copyOf(password, password.length);
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
