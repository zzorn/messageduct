package org.messageduct.utils.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.flowutils.Check;

import java.util.*;

import static org.flowutils.Check.notNull;

/**
 * Serializer using the Kryo library.
 * Not thread safe.
 */
public final class KryoSerializer implements Serializer {

    public static final int DEFAULT_INITIAL_BUFFER_SIZE = 1024;
    public static final int DEFAULT_MAX_BUFFER_SIZE = -1;

    private static final Comparator<Class> CLASS_NAME_COMPARATOR = new Comparator<Class>() {
        @Override public int compare(Class o1, Class o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };


    private final Kryo kryo;
    private final Output outputBuffer;
    private final Input inputBuffer;

    private final List<Class> allowedClasses = new ArrayList<Class>();


    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo,
     * which does not require registration of classes to be serialized in advance.
     */
    public KryoSerializer() {
        this(false);
    }

    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo.
     *
     * @param requireRegistration if true only the specified classes are allowed to be serialized.
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(boolean requireRegistration, Class ... allowedClasses) {
        this(DEFAULT_INITIAL_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE, createDefaultKryo(requireRegistration), Arrays.asList(allowedClasses));
    }

    /**
     * Creates a new KryoSerializer with the default output buffer size limits and a default kryo,
     * which requires registration of classes to be serialized in advance.
     *
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(Collection<Class> allowedClasses) {
        this(DEFAULT_INITIAL_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE, createDefaultKryo(true), allowedClasses);
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
        this(initialBufferSizeBytes, maximumBufferSizeBytes, createDefaultKryo(requireRegistration), Arrays.asList(allowedClasses));
    }

    /**
     * Creates a new KryoSerializer with a default kryo, which requires registration of classes to be serialized in advance.
     *
     * @param initialBufferSizeBytes initial write buffer size.
     * @param maximumBufferSizeBytes maximum write buffer size.  Objects that serialize to longer than this can not be serialized.  Can be -1 for no maximum.
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(int initialBufferSizeBytes, int maximumBufferSizeBytes, Collection<Class> allowedClasses) {
        this(initialBufferSizeBytes, maximumBufferSizeBytes, createDefaultKryo(true), allowedClasses);
    }

    /**
     * Creates a new KryoSerializer.
     *
     * @param initialBufferSizeBytes initial write buffer size.
     * @param maximumBufferSizeBytes maximum write buffer size.  Objects that serialize to longer than this can not be serialized.  Can be -1 for no maximum.
     * @param kryo the kryo instance to use
     * @param allowedClasses Classes that are allowed to be serialized or deserialized.
     */
    public KryoSerializer(int initialBufferSizeBytes, int maximumBufferSizeBytes, Kryo kryo, Collection<Class> allowedClasses) {
        notNull(kryo, "kryo");
        Check.positive(initialBufferSizeBytes, "initialBufferSizeBytes");

        this.kryo = kryo;

        // Register the allowed classes
        if (allowedClasses != null) {
            this.allowedClasses.addAll(allowedClasses);

            // NOTE: Order is important, it needs to be same in each serializer working on the same data.  To get an uniform order, we sort the classes by name:
            Collections.sort(this.allowedClasses, CLASS_NAME_COMPARATOR);

            // Register
            for (Class allowedClass : this.allowedClasses) {
                this.kryo.register(allowedClass);
            }
        }

        // Create buffers
        outputBuffer = new Output(initialBufferSizeBytes, maximumBufferSizeBytes);
        inputBuffer = new Input();
    }

    @Override public byte[] serialize(Object object) {
        // Serialize the object
        kryo.writeClassAndObject(outputBuffer, object);

        // Return new bytearray with data
        return outputBuffer.toBytes();
    }

    @Override public <T> T deserialize(Class<T> expectedType, byte[] data) {
        // Set data to use
        inputBuffer.setBuffer(data);

        // Decode
        return (T) kryo.readClassAndObject(inputBuffer);
    }

    /**
     * @param requireRegistration wether to require registration.
     * @return a new Kryo instance
     */
    public static Kryo createDefaultKryo(boolean requireRegistration) {
        final Kryo kryo = new Kryo();

        kryo.setRegistrationRequired(requireRegistration);

        return kryo;
    }
}
