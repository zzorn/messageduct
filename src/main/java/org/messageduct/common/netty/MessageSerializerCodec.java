package org.messageduct.common.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.flowutils.serializer.ConcurrentSerializer;

import java.util.List;

import static org.flowutils.Check.notNull;

/**
 * Serializes and de-serializes objects with a given serializer.
 */
public final class MessageSerializerCodec extends ByteToMessageCodec<Object> {

    private final ConcurrentSerializer concurrentSerializer;

    /**
     * @param concurrentSerializer serializer used to serialize and de-serialize messages sent and received.
     *                             Should handle concurrent serialization (use ConcurrentSerializerWrapper if needed).
     */
    public MessageSerializerCodec(ConcurrentSerializer concurrentSerializer) {
        notNull(concurrentSerializer, "concurrentSerializer");

        this.concurrentSerializer = concurrentSerializer;
    }

    @Override protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        System.out.println("MessageSerializerCodec.encode");
        System.out.println("msg = " + msg);
        final byte[] serializedMessage = concurrentSerializer.serialize(msg);
        out.writeBytes(serializedMessage);
    }

    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("MessageSerializerCodec.decode");
        // IDEA: This creates a new ByteBufInputStream for each decoded message, we could reduce garbage by creating a custom reusable InputStream that wraps ByteBuf.
        final Object deSerializedObject = concurrentSerializer.deserialize(new ByteBufInputStream(in));
        out.add(deSerializedObject);
        System.out.println("deSerializedObject = " + deSerializedObject);
    }
}
