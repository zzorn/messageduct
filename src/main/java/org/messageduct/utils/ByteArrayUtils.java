package org.messageduct.utils;

import java.util.Arrays;

import static org.flowutils.Check.lessOrEqual;
import static org.flowutils.Check.notNull;

/**
 * Utilities for manipulating byte arrays.
 */
public final class ByteArrayUtils {

    /**
     * @return the specified number of bytes from start of an array, in a new array.
     *         The original array is untouched.
     */
    public static byte[] getFirst(byte[] sourceData, final int bytesToGetFromStart) {
        notNull(sourceData, "sourceData");
        lessOrEqual(bytesToGetFromStart, "bytesToGetFromStart", sourceData.length, "sourceData length");

        return Arrays.copyOf(sourceData, bytesToGetFromStart);
    }

    /**
     * @return the specified number of bytes from the end of an array, in a new array.
     *         The original array is untouched.
     */
    public static byte[] getLast(byte[] sourceData, final int bytesToGetFromEnd) {
        notNull(sourceData, "sourceData");
        lessOrEqual(bytesToGetFromEnd, "bytesToGetFromEnd", sourceData.length, "sourceData length");

        byte[] lastBytes = new byte[bytesToGetFromEnd];

        System.arraycopy(sourceData, sourceData.length - bytesToGetFromEnd, lastBytes, 0, bytesToGetFromEnd);

        return lastBytes;
    }

    /**
     * @return new byte array with the same contents as sourceData, except for the bytesToDropFromStart first bytes removed.
     *         The original array is untouched.
     */
    public static byte[] dropFirst(byte[] sourceData, final int bytesToDropFromStart) {
        notNull(sourceData, "sourceData");
        lessOrEqual(bytesToDropFromStart, "bytesToDropFromStart", sourceData.length, "sourceData length");

        return getLast(sourceData, sourceData.length - bytesToDropFromStart);
    }

    /**
     * @return new byte array with the same contents as sourceData, except for the bytesToDropFromEnd last bytes removed.
     *         The original array is untouched.
     */
    public static byte[] dropLast(byte[] sourceData, final int bytesToDropFromEnd) {
        notNull(sourceData, "sourceData");
        lessOrEqual(bytesToDropFromEnd, "bytesToDropFromEnd", sourceData.length, "sourceData length");

        return getFirst(sourceData, sourceData.length - bytesToDropFromEnd);
    }

    /**
     * @return new byte array with the content of start followed by end.
     */
    public static byte[] concatenate(final byte[] start, final byte[] end) {
        notNull(start, "start");
        notNull(end, "end");

        byte[] concatenated = new byte[end.length + start.length];

        System.arraycopy(start, 0, concatenated, 0, start.length);
        System.arraycopy(end, 0, concatenated, start.length, end.length);

        return concatenated;
    }


    private ByteArrayUtils() {
    }
}
