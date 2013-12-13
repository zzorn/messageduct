package org.messageduct.account.messages;

/**
 *
 */
public abstract class AccountResponseMessageBase implements AccountResponseMessage {

    private final String userName;

    public AccountResponseMessageBase(String userName) {
        this.userName = userName;
    }

    @Override public boolean shouldCloseConnection() {
        return false;
    }

    public final String getUserName() {
        return userName;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "{" +
               "userName='" + userName + '\'' +
               '}';
    }
}
