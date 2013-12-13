package org.messageduct.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities for working with streams.
 */
public final class StreamUtils {

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


    private StreamUtils() {
    }
}
