package org.messageduct.utils;

import java.util.Arrays;

/**
 * Utilities for manipulating byte arrays.
 */
public final class ByteArrayUtils {

    /**
     * @return new byte array with the same contents as sourceData, except for the prefixLengthToRemove first bytes removed.
     */
    public static byte[] removeArrayPrefix(byte[] sourceData, final int prefixLengthToRemove) {

        byte[] newData = new byte[sourceData.length - prefixLengthToRemove];

        System.arraycopy(sourceData, prefixLengthToRemove, newData, 0, newData.length);

        return newData;
    }

    /**
     * @return new byte array with the same contents as sourceData, except for the postfixLengthToRemove last bytes removed.
     */
    public static byte[] removeArrayPostfix(byte[] sourceData, final int postfixLengthToRemove) {

        return Arrays.copyOf(sourceData, sourceData.length - postfixLengthToRemove);
    }

    /**
     * @return new byte array with the content of start followed by end.
     */
    public static byte[] concatenateByteArrays(final byte[] start, final byte[] end) {

        byte[] concatenated = new byte[end.length + start.length];

        System.arraycopy(start, 0, concatenated, 0, start.length);
        System.arraycopy(end, 0, concatenated, start.length, end.length);

        return concatenated;
    }


    private ByteArrayUtils() {
    }
}
