package org.messageduct.account.messages;

import org.messageduct.utils.SecurityUtils;

/**
 * Message to execute a previously requested account delete.
 */
public class DeleteAccountExecuteMessage extends AccountMessageBase {

    private final char[] deletionCode;

    /**
     * @param username account to delete.
     * @param password password of user.
     * @param deletionCode account delete code sent to user.
     */
    public DeleteAccountExecuteMessage(String username, char[] password, char[] deletionCode) {
        super(username, password);
        this.deletionCode = deletionCode;
    }

    public char[] getDeletionCode() {
        return deletionCode;
    }

    @Override public void scrubPassword() {
        super.scrubPassword();
        SecurityUtils.scrubChars(deletionCode);
    }
}
