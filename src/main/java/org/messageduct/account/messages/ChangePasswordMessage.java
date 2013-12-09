package org.messageduct.account.messages;

import org.messageduct.utils.SecurityUtils;

/**
 * Message used to change password.
 */
public class ChangePasswordMessage extends AccountMessageBase {

    private final char[] newPassword;

    /**
     * @param username username to change password for.  Should be the same as the currently logged in user.
     * @param oldPassword old password
     * @param newPassword new password
     */
    public ChangePasswordMessage(String username, char[] oldPassword, char[] newPassword) {
        super(username, oldPassword);
        this.newPassword = newPassword;
    }

    public char[] getNewPassword() {
        return newPassword;
    }

    @Override public void scrubPassword() {
        super.scrubPassword();
        SecurityUtils.scrubChars(newPassword);
    }
}
