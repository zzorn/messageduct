package org.messageduct.utils;

/**
 * Checks that a password is acceptable.
 */
public interface PasswordValidator {

    /**
     * @param password the password to check.
     * @return null if the password is acceptable, an error message if it is not.
     */
    String check(char[] password, String userName);

}
