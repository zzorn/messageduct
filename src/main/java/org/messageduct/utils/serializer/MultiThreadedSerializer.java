package org.messageduct.utils.serializer;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 * Multithreaded wrapper that allows any serializer to be used in a multithreaded context.
 *
 * One serializer instance is created for each thread.
 */
public final class MultiThreadedSerializer implements Serializer {

    private final SerializerFactory serializerFactory;
    private final ThreadLocal<Serializer> serializers = new ThreadLocal<Serializer>();

    /**
     * @param serializerClass type of serializer to create.  Will call the no-argument constructor.
     */
    public MultiThreadedSerializer(final Class<? extends Serializer> serializerClass) {
        notNull(serializerClass, "serializerClass");

        // Create factory that creates new instances from the specified class
        this.serializerFactory = new SerializerFactory() {
            @Override public Serializer createSerializer() {
                try {
                    return serializerClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Serializer could not be created for class '"+serializerClass+"': " + e.getMessage(), e);
                }
            }
        };
    }

    /**
     * @param serializerFactory factory used to create serializers.
     */
    public MultiThreadedSerializer(SerializerFactory serializerFactory) {
        notNull(serializerFactory, "serializerFactory");

        this.serializerFactory = serializerFactory;
    }

    @Override public byte[] serialize(Object object) {
        return getLocalSerializer().serialize(object);
    }

    @Override public <T> T deserialize(Class<T> expectedType, byte[] data) {
        return getLocalSerializer().deserialize(expectedType, data);
    }

    /**
     * @return serializer that is unique for the current thread.
     */
    public Serializer getLocalSerializer() {
        // Get local serializer if it exists
        Serializer serializer = serializers.get();

        // Create if it does not yet exist
        if (serializer == null) {
            // Create serializer
            serializer = serializerFactory.createSerializer();
            if (serializer == null) throw new IllegalStateException("SerializerFactory '"+serializerFactory.getClass()+"' provided a null instead of a serializer.");

            // Remember it
            serializers.set(serializer);
        }

        return serializer;
    }

}
