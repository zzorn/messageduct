package org.messageduct.account.model;

/**
 * Default user account.
 *
 * Can be extended by the application to provide more account specific data.
 */
public class DefaultAccount implements Account {

    private final String userName;
    private String passwordHash;
    private String email;
    private String publicKey;
    private String bitcoinAddress;

    /**
     * Only used by serialization.
     */
    private DefaultAccount() {
        userName = null;
    }

    public DefaultAccount(String userName, String passwordHash) {
        this(userName, passwordHash, null);
    }

    public DefaultAccount(String userName, String passwordHash, String email) {
        this(userName, passwordHash, email, null, null);
    }

    public DefaultAccount(String userName, String passwordHash, String email, String publicKey, String bitcoinAddress) {
        this.userName = userName;
        this.passwordHash = passwordHash;
        this.email = email;
        this.publicKey = publicKey;
        this.bitcoinAddress = bitcoinAddress;
    }

    @Override public String getUserName() {
        return userName;
    }

    @Override public String getPasswordHash() {
        return passwordHash;
    }

    @Override public String getEmail() {
        return email;
    }

    @Override public String getPublicKey() {
        return publicKey;
    }

    @Override public String getBitcoinAddress() {
        return bitcoinAddress;
    }


}
