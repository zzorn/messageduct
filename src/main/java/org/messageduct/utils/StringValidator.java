package org.messageduct.utils;

/**
 * Checks that some string adheres to some rules.
 */
public interface StringValidator {

    /**
     * @param s string to test.
     * @return null if s is valid, error description if s is not valid.
     */
    String check(String s);

}
