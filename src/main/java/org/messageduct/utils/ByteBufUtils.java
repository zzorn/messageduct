package org.messageduct.utils;

import io.netty.buffer.ByteBuf;

/**
 *
 */
public final class ByteBufUtils {

    public static byte[] byteBufToByteArray(ByteBuf buffer) {
        byte[] byteArray = new byte[buffer.readableBytes()];
        if (byteArray.length > 0) buffer.readBytes(byteArray);
        return byteArray;
    }

    private ByteBufUtils() {
    }
}
