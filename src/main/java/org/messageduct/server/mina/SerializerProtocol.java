package org.messageduct.server.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;
import org.messageduct.utils.serializer.ConcurrentSerializer;

/**
 * Uses a specified serializer to encode classes for transport over the network.
 */
public final class SerializerProtocol implements ProtocolCodecFactory {

    private final Encoder encoder;
    private final Decoder decoder;

    /**
     * Creates a SerializerProtocol that uses the specified serializer.
     *
     * @param concurrentSerializer serializer that allows simultaneous use from multiple threads.
     */
    public SerializerProtocol(ConcurrentSerializer concurrentSerializer) {
        encoder = new Encoder(concurrentSerializer);
        decoder = new Decoder(concurrentSerializer);
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }

    public final static class Encoder extends ProtocolEncoderAdapter {

        private final ConcurrentSerializer concurrentSerializer;

        public Encoder(ConcurrentSerializer concurrentSerializer) {
            this.concurrentSerializer = concurrentSerializer;
        }

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            System.out.println("SerializerProtocol$Encoder.encode " + message);

            // Serialize object
            final byte[] data = concurrentSerializer.serialize(message);

            // Write serialized data to mina as an IOBuffer
            out.write(IoBuffer.wrap(data));
        }
    }

    public final static class Decoder extends ProtocolDecoderAdapter {

        private final ConcurrentSerializer concurrentSerializer;

        public Decoder(ConcurrentSerializer concurrentSerializer) {
            this.concurrentSerializer = concurrentSerializer;
        }

        @Override
        public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            System.out.println("SerializerProtocol$Decoder.decode");

            // Decode the message from a stream
            final Object message = concurrentSerializer.deserialize(in.asInputStream());

            // Pass message to next handler
            out.write(message);
        }
    }

}
