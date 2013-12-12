package org.messageduct.utils.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.flowutils.Check;

import java.io.InputStream;
import java.util.*;

import static org.flowutils.Check.notNull;

/**
 * Serializer using the Kryo library.
 * Not thread safe.
 */
public final class KryoSerializer extends SerializerBase {

    public static final int DEFAULT_INITIAL_BUFFER_SIZE = 1024;
    public static final int DEFAULT_MAX_BUFFER_SIZE = -1;

    private final Kryo kryo;
    private final int initialBufferSizeBytes;
    private final int maximumBufferSizeBytes;
    private Output outputBuffer;
    private Input inputBuffer;


    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo,
     * which requires registration of classes to be serialized in advance.
     */
    public KryoSerializer() {
        this(true);
    }

    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo.
     * @param requireRegistration if true only the specified classes are allowed to be serialized.
     */
    public KryoSerializer(boolean requireRegistration) {
        this(DEFAULT_INITIAL_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE, requireRegistration);
    }

    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo,
     * which requires registration of classes to be serialized in advance.
     *
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(Class ... allowedClasses) {
        this(Arrays.asList(allowedClasses));
    }

    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo,
     * which requires registration of classes to be serialized in advance.
     *
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(Collection<Class> allowedClasses) {
        this(DEFAULT_INITIAL_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE, createDefaultKryo(), true, allowedClasses);
    }

    /**
     * Creates a new KryoSerializer with a default kryo.
     *
     * @param initialBufferSizeBytes initial write buffer size.
     * @param maximumBufferSizeBytes maximum write buffer size.  Objects that serialize to longer than this can not be serialized.  Can be -1 for no maximum.
     * @param requireRegistration if true only the specified classes are allowed to be serialized.
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(int initialBufferSizeBytes, int maximumBufferSizeBytes, boolean requireRegistration, Class ... allowedClasses) {
        this(initialBufferSizeBytes, maximumBufferSizeBytes, createDefaultKryo(), requireRegistration, Arrays.asList(allowedClasses));
    }

    /**
     * Creates a new KryoSerializer with a default kryo.
     *
     * @param initialBufferSizeBytes initial write buffer size.
     * @param maximumBufferSizeBytes maximum write buffer size.  Objects that serialize to longer than this can not be serialized.  Can be -1 for no maximum.
     * @param requireRegistration if true only the specified classes are allowed to be serialized.
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(int initialBufferSizeBytes, int maximumBufferSizeBytes, boolean requireRegistration, Collection<Class> allowedClasses) {
        this(initialBufferSizeBytes, maximumBufferSizeBytes, createDefaultKryo(), requireRegistration, allowedClasses);
    }

    /**
     * Creates a new KryoSerializer.
     *
     * @param initialBufferSizeBytes initial write buffer size.
     * @param maximumBufferSizeBytes maximum write buffer size.  Objects that serialize to longer than this can not be serialized.  Can be -1 for no maximum.
     * @param kryo the kryo instance to use
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(int initialBufferSizeBytes, int maximumBufferSizeBytes, Kryo kryo, boolean requireRegistration, Collection<Class> allowedClasses) {
        super(requireRegistration, allowedClasses);

        notNull(kryo, "kryo");
        Check.positive(initialBufferSizeBytes, "initialBufferSizeBytes");

        this.kryo = kryo;
        this.initialBufferSizeBytes = initialBufferSizeBytes;
        this.maximumBufferSizeBytes = maximumBufferSizeBytes;
    }


    @Override protected void initialize(boolean registrationRequired, List<Class> registeredClasses) {
        // Specify whether classes need to be registered in advance to be serialized
        this.kryo.setRegistrationRequired(registrationRequired);

        // Register the allowed classes
        for (Class registeredClass : registeredClasses) {
            kryo.register(registeredClass);
        }

        // Create buffers
        outputBuffer = new Output(initialBufferSizeBytes, maximumBufferSizeBytes);
        inputBuffer = new Input();
    }

    @Override protected byte[] doSerialize(Object object) {
        // Clear buffer to deserialize to
        outputBuffer.clear();

        // Serialize the object
        kryo.writeClassAndObject(outputBuffer, object);

        // Return new bytearray with data
        return outputBuffer.toBytes();
    }

    @Override protected <T> T doDeserialize(Class<T> expectedType, byte[] data) {
        // Set data array to use
        inputBuffer.setBuffer(data);

        // Decode
        return (T) kryo.readClassAndObject(inputBuffer);
    }

    @Override protected <T> T doDeserialize(Class<T> expectedType, InputStream inputStream) {
        // Set data stream to use
        inputBuffer.setInputStream(inputStream);

        // Decode
        return (T) kryo.readClassAndObject(inputBuffer);
    }

    /**
     * @return a new Kryo instance
     */
    public static Kryo createDefaultKryo() {
        return new Kryo();
    }
}
