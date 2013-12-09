package org.messageduct.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.util.*;

/**
 * Uses Kryo to encode classes in a binary form.
 */
// TODO: Will there be issues if the session is used from many threads?  Pass in a multithreaded serializer instead
public class BinaryProtocol implements ProtocolCodecFactory {

    private static final String BINARY_ENCODER = "BINARY_ENCODER";
    private static final String BINARY_DECODER = "BINARY_DECODER";

    private static final Comparator<Class> CLASS_NAME_COMPARATOR = new Comparator<Class>() {
        @Override public int compare(Class o1, Class o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private final int startBufferSizeKb;
    private final int maxBufferSizeKb;
    private final List<Class> allowedClasses = new ArrayList<Class>();

    /**
     * Creates a BinaryProtocol with a 1kb initial object output buffer size, up to a maximum of 16 kb.
     *
     * @param allowedClasses the classes that are allowed to be serialized and transferred over the network.
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     */
    public BinaryProtocol(Set<Class> allowedClasses) {
        this(allowedClasses, 1,  16);
    }

    /**
     * @param allowedClasses the classes that are allowed to be serialized and transferred over the network.
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     * @param bufferSizeKb object serialization output buffer size in kilobytes.
     */
    public BinaryProtocol(Set<Class> allowedClasses, int bufferSizeKb) {
        this(allowedClasses, bufferSizeKb,  bufferSizeKb);
    }

    /**
     * @param allowedClasses the classes that are allowed to be serialized and transferred over the network.
     * NOTE: It's important that the server and client have exactly the same allowed classes!
     * @param startBufferSizeKb initial object output buffer size in kilobytes.
     * @param maxBufferSizeKb maximum object output buffer size in kilobytes.
     */
    public BinaryProtocol(Set<Class> allowedClasses, final int startBufferSizeKb, final int maxBufferSizeKb) {
        this.allowedClasses.addAll(allowedClasses);
        this.startBufferSizeKb = startBufferSizeKb;
        this.maxBufferSizeKb = maxBufferSizeKb;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        // Store encoders per session
        ProtocolEncoder binaryEncoder = (ProtocolEncoder) session.getAttribute(BINARY_ENCODER);

        if (binaryEncoder == null) {
            binaryEncoder = new BinaryEncoder(createKryo(), startBufferSizeKb, maxBufferSizeKb);
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

    protected Kryo createKryo() {
        Kryo kryo = new Kryo();

        // Only allow serialization of specified classes
        kryo.setRegistrationRequired(true);

        // Register the allowed classes
        // NOTE: Order is important, it needs to be same on client and server.  To get an uniform order, we sort the classes by name:
        Collections.sort(allowedClasses, CLASS_NAME_COMPARATOR);
        for (Class allowedClass : allowedClasses) {
            kryo.register(allowedClass);
        }

        return kryo;
    }

    public final static class BinaryEncoder extends ProtocolEncoderAdapter {
        private final Kryo kryo;
        private final Output outputBuffer;

        public BinaryEncoder(Kryo kryo, final int startBufferSizeKb, final int maxBufferSizeKb) {
            this.kryo = kryo;
            outputBuffer = new Output(startBufferSizeKb *1024, maxBufferSizeKb *1024);
        }

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            // Use Kryo to serialize the object
            kryo.writeObject(outputBuffer, message);

            // Write object to mina as IOBuffer
            out.write(IoBuffer.wrap(outputBuffer.toBytes()));
        }
    }

    public final static class BinaryDecoder extends ProtocolDecoderAdapter {
        private final Kryo kryo;
        private final Input input;

        public BinaryDecoder(Kryo kryo) {
            this.kryo = kryo;
            input = new Input();
        }

        @Override
        public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            // Specify input data to use when decoding
            if (in.hasArray()) input.setBuffer(in.array());
            else input.setInputStream(in.asInputStream());

            // Decode the message
            final Object message = kryo.readClassAndObject(input);

            // Pass message to next handler
            out.write(message);
        }
    }

}
