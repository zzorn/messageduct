package org.messageduct.utils;

import org.flowutils.Check;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Security related utilities.
 */
public final class SecurityUtils {

    private static final int SCRUB_LOOP_COUNT = 8;

    private static Random secureRandom = new SecureRandom();
    private static Random pseudoRandom = new Random();

    /**
     * Overwrite the memory of the specified array with random characters, scrubbing it clear.
     * Useful for removing passwords from memory when they are no longer needed.
     * @param s char array to overwrite.
     */
    public static void scrubChars(final char[] s) {
        for (int k = 0; k < SCRUB_LOOP_COUNT; k++) {
            for (int i = 0; i < s.length; i++) {
                s[i] = randomChar();
            }
        }
    }

    /**
     * @return a new random character from the full unicode range.
     */
    public static char randomChar() {
        // Maximize entropy by mixing two random implementations
        return (char) (secureRandom.nextInt(Character.MAX_VALUE) ^
                       pseudoRandom.nextInt(Character.MAX_VALUE));
    }

    /**
     * @return a new random character in the ascii range, not including control codes.
     */
    public static char randomAsciiChar() {
        return (char) (32+ secureRandom.nextInt(128-32));
    }

    /**
     * @return a new array with random characters in the ascii range, not including control codes.
     */
    public static char[] randomAsciiChars(int length) {
        Check.positiveOrZero(length, "length");

        final char[] chars = new char[length];

        for (int i = 0; i < chars.length; i++) {
            chars[i] = randomAsciiChar();
        }

        return chars;
    }

    // Static class
    private SecurityUtils() {
    }
}
