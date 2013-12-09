package org.messageduct.utils;

/**
 * Hashes passwords, and compares passwords to a hash.
 */
public interface PasswordHasher {

    /**
     * @param password password to hash.
     * @return a hash for the specified password.
     */
    String hashPassword(char[] password);

    /**
     * @param password password to test.
     * @param hash hash to test the password against.
     * @return true if the password hashes to the specified hash, false if the password is incorrect.
     */
    boolean isCorrectPassword(char[] password, String hash);

}
