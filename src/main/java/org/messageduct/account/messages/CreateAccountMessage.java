package org.messageduct.account.messages;

/**
 * Message sent by the client to the server to create a new account.
 */
public class CreateAccountMessage extends AccountMessageBase implements NonAuthenticatedAccountMessage {

    private final String email;
    private final String userPublicKey;

    /**
     * @param username desired username
     * @param password desired password
     */
    public CreateAccountMessage(String username, char[] password) {
        this(username, password, null);
    }

    /**
     * @param username desired username
     * @param password desired password
     * @param email email, or null if none provided.
     */
    public CreateAccountMessage(String username, char[] password, String email) {
        this(username, password, email, null);
    }

    /**
     * @param username desired username
     * @param password desired password
     * @param email email, or null if none provided.
     * @param userPublicKey public key, or null if none provided.
     */
    public CreateAccountMessage(String username,
                                char[] password,
                                String email,
                                String userPublicKey) {
        super(username, password);
        this.email = email;
        this.userPublicKey = userPublicKey;
    }

    /**
     * @return email provided by the user, or null if none provided.
     * Can be used for sending password reset codes to, or general announcements or status updates.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return a public key specified by the client.
     * Can be used as an alternative to email verification, by asking the user to sign a nonce sent by the server,
     * or by providing an encrypted password reset code or similar.
     */
    public String getUserPublicKey() {
        return userPublicKey;
    }

}
