package org.messageduct.account.messages;

/**
 * Message indicating some error condition related to account actions, sent from server to client.
 */
public final class AccountErrorMessage implements AccountResponseMessage {
    private final String errorType;
    private final String errorMessage;
    private final boolean closeConnection;

    public AccountErrorMessage(String errorType) {
        this(errorType, null);
    }

    public AccountErrorMessage(String errorType, String errorMessage) {
        this(errorType, errorMessage, false);
    }

    public AccountErrorMessage(String errorType, String errorMessage, boolean closeConnection) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.closeConnection = closeConnection;
    }

    /**
     * @return the type of the error, usually a fixed string.
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @return Semi user readable message describing the error.
     */
    public String getErrorMessage() {
        if (errorMessage == null) return errorType;
        else return errorMessage;
    }

    public boolean shouldCloseConnection() {
        return closeConnection;
    }

    @Override public String toString() {
        return "Error: " + errorType + ": " + errorMessage + ".";
    }
}
