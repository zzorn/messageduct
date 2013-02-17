package org.messageduct.message;

/**
 * Message indicating some error condition, usually sent from server to client.
 */
public class ErrorMessage implements Message {
    private final String errorType;
    private final String errorMessage;

    public ErrorMessage(String errorType) {
        this(errorType, null);
    }

    public ErrorMessage(String errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
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
}
