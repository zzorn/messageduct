package org.messageduct.account.messages;

import org.messageduct.utils.SecurityUtils;

/**
 * Message to execute a previously requested password reset.
 */
public class PasswordResetExecuteMessage extends AccountMessageBase implements NonAuthenticatedAccountMessage {

    private final char[] resetCode;

    /**
     * @param username account to reset password for.
     * @param resetCode password reset code sent to user.
     * @param newPassword new password to set to user.
     */
    public PasswordResetExecuteMessage(String username, char[] resetCode, char[] newPassword) {
        super(username, newPassword);
        this.resetCode = resetCode;
    }

    public char[] getResetCode() {
        return resetCode;
    }

    @Override public void scrubPassword() {
        super.scrubPassword();
        SecurityUtils.scrubChars(resetCode);
    }
}
