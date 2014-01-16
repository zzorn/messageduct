package org.messageduct.utils;

import java.io.*;

import static org.flowutils.Check.notNull;

/**
 * Utilities for working with streams.
 */
public final class StreamUtils {

    /**
     * Default maximum length of byte arrays that will be read from streams.
     */
    public static final int DEFAULT_MAX_BYTE_ARRAY_LENGTH = 10*1024*1024;  // 10 MB

    /**
     * Reads the whole input stream and returns the bytes in a byte array.
     *
     * @param inputStream input stream to read.
     * @return the bytes contained in the input stream.
     * @throws IOException thrown if there was some problem reading the input stream
     */
    public static byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        // Byte array output stream to write to
        final ByteArrayOutputStream output= new ByteArrayOutputStream(1024);

        // Temporary buffer used to ferry bytes from the input to the output
        final byte[] tempBuffer = new byte[1024];

        // Copy everything from the input to the output
        while (true) {
            int len = inputStream.read(tempBuffer);
            if (len == -1) break; // No more data
            output.write(tempBuffer, 0, len);
        }

        // Return collected bytes from output
        output.flush();
        output.close();
        return output.toByteArray();
    }



    /**
     * Reads a byte array from an input stream.
     * The byte array has its length prefixed to it, as a 32 bit java integer in network byte order.
     *
     * The array can be at most DEFAULT_MAX_BYTE_ARRAY_LENGTH (10 MB) bytes long, if longer, an IOException is thrown.
     * Use the other readByteArray method if you want to specify the max length yourself.
     *
     * @param inputStream Stream to read from
     * @return the read array
     * @throws IOException thrown if there was problem reading the data, or if there was some error in the data,
     *                     or if the array was too large, or if the stream ended.
     */
    public static byte[] readByteArray(InputStream inputStream) throws IOException {
        return readByteArray(inputStream, DEFAULT_MAX_BYTE_ARRAY_LENGTH);
    }

    /**
     * Reads a byte array from an input stream.
     * The byte array has its length prefixed to it, as a 32 bit java integer in network byte order.
     *
     * @param inputStream Stream to read from
     * @param maxLength maximum length allowed for the read byte array.  If longer, an IOException is thrown.
     * @return the read array
     * @throws IOException thrown if there was problem reading the data, or if there was some error in the data,
     *                     or if the array was too large, or if the stream ended.
     */
    public static byte[] readByteArray(InputStream inputStream, int maxLength) throws IOException {
        // Read length
        final int length = readInt(inputStream);

        // Sanity check length
        if (length < 0) throw new IOException("Invalid byte array length provided: " + length);
        if (length > maxLength) throw new IOException("Too large byte array length provided: " + length + ", maximum length is " + maxLength);

        // Read the array
        byte[] data = new byte[length];
        final int readLength = inputStream.read(data);

        // Sanity check what we read
        if (readLength < 0) throw new IOException("Could not read data for byte array, end of stream reached");
        if (readLength != length) throw new IOException("Could not read all data for byte array, expected " + length + " " +
                                                        "but only got " + readLength + " bytes");
        return data;
    }

    /**
     * Writes a byte array to an output stream.
     * The byte array has its length prefixed to it, as a 32 bit java integer in network byte order.
     *
     *
     * @param outputStream stream to write to.
     * @param data data to write.  Should not be null.
     * @throws IOException thrown if there was problem writing the data.
     */
    public static void writeByteArray(OutputStream outputStream, byte[] data) throws IOException {
        notNull(data, "data");

        // Write array size
        final int dataLength = data.length;
        writeInt(outputStream, dataLength);

        // Write data
        if (dataLength > 0) {
            outputStream.write(data);
        }
    }

    /**
     * Reads a java integer from an input stream, assuming the bytes are in network byte order.
     *
     * @param inputStream input stream to read from
     * @return the read integer
     * @throws IOException thrown if there was some error when reading the integer, or if the stream ended.
     */
    public static int readInt(InputStream inputStream) throws IOException {
        // Read in network byte order = big endian = most significant byte first.
        int result = 0;
        result |= (readByte(inputStream) & 0xFF) << 24;
        result |= (readByte(inputStream) & 0xFF) << 16;
        result |= (readByte(inputStream) & 0xFF) << 8;
        result |= (readByte(inputStream) & 0xFF);

        return result;
    }

    /**
     * Writes a java integer to an output stream in network byte order.
     *
     * @param outputStream stream to write to
     * @param value integer to write
     * @throws IOException thrown if there was some error when writing the integer.
     */
    public static void writeInt(OutputStream outputStream, int value) throws IOException {
        // Store in network byte order = big endian = most significant byte first.
        outputStream.write((value >> 24) & 0xFF);
        outputStream.write((value >> 16) & 0xFF);
        outputStream.write((value >> 8) & 0xFF);
        outputStream.write(value & 0xFF);
    }

    /**
     * Reads a byte from an input stream.
     *
     * @param inputStream input stream to read from
     * @return the read byte (stored in an integer).
     *         Will not return -1 if the stream ended like inputStream.read, throws an IOException in that case instead.
     * @throws IOException thrown if there was some error when reading the integer, or if the stream ended.
     */
    public static int readByte(InputStream inputStream) throws IOException {
        final int inputByte = inputStream.read();

        if (inputByte < 0) throw new IOException("Unexpected end of input");

        return inputByte;
    }


    private StreamUtils() {
    }
}
