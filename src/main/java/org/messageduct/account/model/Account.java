package org.messageduct.account.model;

/**
 * Data about an user account.
 */
public interface Account {

    /**
     * @return account name.  Must be unique and not null.
     */
    String getUserName();

    /**
     * @return hash of the users password, for checking password on login.
     */
    String getPasswordHash();

    /**
     * @return users email, if provided, null otherwise.
     */
    String getEmail();

    /**
     * @return public key for user, if provided, null otherwise.
     */
    String getPublicKey();

    /**
     * @return bitcoin address for user, if provided, null otherwise.
     */
    String getBitcoinAddress();
}
