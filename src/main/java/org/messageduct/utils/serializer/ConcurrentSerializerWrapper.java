package org.messageduct.utils.serializer;

import java.io.InputStream;
import java.util.List;

import static org.flowutils.Check.notNull;

/**
 * Multithreaded wrapper that allows any serializer to be used in a multithreaded context.
 * One serializer instance is created for each thread.
 */
public final class ConcurrentSerializerWrapper extends SerializerBase implements ConcurrentSerializer {

    private final SerializerFactory serializerFactory;
    private final ThreadLocal<Serializer> serializers = new ThreadLocal<Serializer>();

    private List<Class> registeredClasses = null;
    private boolean registrationRequired;

    /**
     * Creates a new Multithreaded serializer that uses the specified serializer class to create serializer instances for each thread.
     *
     * @param serializerClass type of serializer to create.  Will call the no-argument constructor.
     */
    public ConcurrentSerializerWrapper(final Class<? extends Serializer> serializerClass) {
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
     * Creates a new Multithreaded serializer that uses the specified serializer factory to create serializers for each thread.
     *
     * @param serializerFactory factory used to create serializers.
     */
    public ConcurrentSerializerWrapper(SerializerFactory serializerFactory) {
        notNull(serializerFactory, "serializerFactory");

        this.serializerFactory = serializerFactory;
    }

    @Override protected void initialize(boolean registrationRequired, List<Class> registeredClasses) {
        this.registeredClasses = registeredClasses;
        this.registrationRequired = registrationRequired;
    }

    @Override protected byte[] doSerialize(Object object) {
        return getLocalSerializer().serialize(object);
    }

    @Override protected <T> T doDeserialize(Class<T> expectedType, byte[] data) {
        return getLocalSerializer().deserialize(expectedType, data);
    }

    @Override protected <T> T doDeserialize(Class<T> expectedType, InputStream inputStream) {
        return getLocalSerializer().deserialize(expectedType, inputStream);
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

            // Setup serializer
            serializer.setRequireRegistration(registrationRequired);
            serializer.registerAllowedClasses(registeredClasses);

            // Remember it
            serializers.set(serializer);
        }

        return serializer;
    }

}
