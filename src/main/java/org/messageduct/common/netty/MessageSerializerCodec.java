package org.messageduct.common.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.flowutils.Check;
import org.flowutils.serializer.KryoSerializer;
import org.flowutils.serializer.Serializer;
import org.messageduct.utils.ByteBufUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.flowutils.Check.identifier;
import static org.flowutils.Check.notNull;
import static org.flowutils.Check.strictIdentifier;

/**
 * Serializes and de-serializes objects with a given serializer.
 */
public final class MessageSerializerCodec extends ByteToMessageCodec<Object> {

    private final Serializer serializer;

    /**
     * @param maxMessageSize maximum allowed serialized message size.  Used to reserve a serialization buffer.
     * @param allowedClasses only classes of these types and primitive types are allowed to be serialized.
     */
    public MessageSerializerCodec(int maxMessageSize, Collection<Class> allowedClasses) {
        this(new KryoSerializer(maxMessageSize/8, maxMessageSize, false, allowedClasses));
        Check.positive(maxMessageSize, "maxMessageSize");
    }

    /**
     * @param serializer serializer used to serialize and de-serialize messages sent and received.
     *                             Should handle concurrent serialization (use ConcurrentSerializerWrapper if needed).
     */
    public MessageSerializerCodec(Serializer serializer) {
        notNull(serializer, "serializer");

        this.serializer = serializer;
    }

    @Override protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // Ignore null messages
        if (msg != null) {
            // TODO: OPTIMIZE: This creates a new byte array for each encoded message.  Reduce that somehow.
            final byte[] serializedMessage = serializer.serialize(msg);
            out.writeBytes(serializedMessage);
        }
    }

    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final byte[] data = ByteBufUtils.byteBufToByteArray(in);
        // TODO: OPTIMIZE: This creates a new byte array for each decoded message.  Reduce that somehow.
        final Object deSerializedObject = serializer.deserialize(data);

        // Ignore null messages
        if (deSerializedObject != null) {
            out.add(deSerializedObject);
        }
    }
}
