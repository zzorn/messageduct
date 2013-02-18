package org.messageduct.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;

import java.io.ByteArrayOutputStream;

/**
 *
 */
public class BinaryProtocol implements ProtocolCodecFactory {

    public static final String BINARY_ENCODER = "BINARY_ENCODER";
    public static final String BINARY_DECODER = "BINARY_DECODER";

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        // Store encoders per session
        ProtocolEncoder binaryEncoder = (ProtocolEncoder) session.getAttribute(BINARY_ENCODER);

        if (binaryEncoder == null) {
            binaryEncoder = new BinaryEncoder(createKryo());
            session.setAttribute(BINARY_ENCODER, binaryEncoder);
        }

        return binaryEncoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        // Store decoders per session
        ProtocolDecoder binaryDecoder = (ProtocolDecoder) session.getAttribute(BINARY_DECODER);

        if (binaryDecoder == null) {
            binaryDecoder = new BinaryDecoder(createKryo());
            session.setAttribute(BINARY_DECODER, binaryDecoder);
        }

        return binaryDecoder;
    }

    private Kryo createKryo() {
        Kryo kryo = new Kryo();

        // TODO: Register allowed classes etc.

        return kryo;
    }

    public final static class BinaryEncoder extends ProtocolEncoderAdapter {
        private final Kryo kryo;

        public BinaryEncoder(Kryo kryo) {
            this.kryo = kryo;
        }

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            //kryo.writeObject(new Output(new ByteArrayOutputStream()), message);
            //out.write();
            // TODO
        }
    }

    public final static class BinaryDecoder extends ProtocolDecoderAdapter {
        private final Kryo kryo;

        public BinaryDecoder(Kryo kryo) {
            this.kryo = kryo;
        }

        @Override
        public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            // TODO
        }
    }

}
