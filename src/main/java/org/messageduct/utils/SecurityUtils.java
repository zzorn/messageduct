package org.messageduct.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Security related utilities.
 */
public final class SecurityUtils {

    private static final int LOOP_COUNT = 8;
    private static Random semiSecureRandom = new SecureRandom();
    private static Random pseudoRandom = new Random();

    /**
     * Overwrite the memory of the specified array with random characters, scrubbing it clear.
     * Useful for removing passwords from memory when they are no longer needed.
     * @param s char array to overwrite.
     */
    public static void scrubChars(final char[] s) {
        for (int k = 0; k < LOOP_COUNT; k++) {
            for (int i = 0; i < s.length; i++) {
                s[i] = randomChar();
            }
        }
    }

    /**
     * @return a new random character.
     */
    public static char randomChar() {
        // Maximize entropy by mixing two random implementations
        return (char) (semiSecureRandom.nextInt(Character.MAX_VALUE) ^
                       pseudoRandom.nextInt(Character.MAX_VALUE));
    }

    // Static class
    private SecurityUtils() {
    }
}
