package org.messageduct.utils.serializer;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.Check;

import java.io.InputStream;
import java.util.*;

import static org.flowutils.Check.notNull;

/**
 * Base class with common functionality for serializers.
 */
public abstract class SerializerBase implements Serializer {

    private final Set<Class> allowedClasses = new ConcurrentHashSet<Class>();
    private boolean requireRegistration = true;
    private boolean initialized = false;

    private final Object initializationLock = new Object();

    private static final Comparator<Class> CLASS_NAME_COMPARATOR = new Comparator<Class>() {
        @Override public int compare(Class o1, Class o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };


    /**
     * @param requireRegistration true if only the registered allowed classes are allowed to be serialized.
     *                            false  if any class should be allowed to be serialized,
     */
    protected SerializerBase(boolean requireRegistration) {
        this(requireRegistration, Collections.<Class>emptyList());
    }

    /**
     * Creates a new serializer that only allows the specified classes to be serialized.
     *
     * @param allowedClasses the classes that are allowed to be serialized.
     */
    protected SerializerBase(Class ... allowedClasses) {
        this(true, Arrays.asList(allowedClasses));
    }

    /**
     * Creates a new serializer that only allows the specified classes to be serialized.
     *
     * @param allowedClasses the classes that are allowed to be serialized.
     */
    protected SerializerBase(Collection<Class> allowedClasses) {
        this(true, allowedClasses);
    }

    /**
     * @param requireRegistration true if only the registered allowed classes are allowed to be serialized.
     *                            false  if any class should be allowed to be serialized,
     * @param allowedClasses the classes that can be serialized if all classes are not allowed.
     */
    protected SerializerBase(boolean requireRegistration, Collection<Class> allowedClasses) {
        this.requireRegistration = requireRegistration;
        registerAllowedClasses(allowedClasses);
    }

    @Override public final boolean isRequireRegistration() {
        return requireRegistration;
    }

    @Override public final void setRequireRegistration(boolean requireRegistration) {
        checkNotInitialized();
        this.requireRegistration = requireRegistration;
    }

    @Override public void registerCommonCollectionClasses() {
        registerAllowedClasses(ArrayList.class,
                               LinkedList.class,
                               HashMap.class,
                               HashSet.class,
                               LinkedHashMap.class,
                               LinkedHashSet.class);
    }

    @Override public final void registerAllowedClass(Class allowedClass) {
        checkNotInitialized();
        notNull(allowedClass, "allowedClass");

        allowedClasses.add(allowedClass);
    }

    @Override public final void registerAllowedClass(String className) {
        checkNotInitialized();
        notNull(className, "className");

        try {
            registerAllowedClass(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No class named '"+className+"' found: " + e.getMessage(), e);
        }
    }

    @Override public final void registerAllowedClasses(Class... allowedClasses) {
        checkNotInitialized();
        for (Class allowedClass : allowedClasses) {
            registerAllowedClass(allowedClass);
        }
    }

    @Override public final void registerAllowedClasses(Collection<Class> allowedClasses) {
        checkNotInitialized();
        Check.notNull(allowedClasses, "allowedClasses");

        for (Class allowedClass : allowedClasses) {
            registerAllowedClass(allowedClass);
        }
    }

    @Override public final Set<Class> getAllowedClasses() {
        return Collections.unmodifiableSet(allowedClasses);
    }

    @Override public final byte[] serialize(Object object) {
        initializeIfNeeded();

        return doSerialize(object);
    }

    @Override public final <T> T deserialize(byte[] data) {
        return (T) deserialize(Object.class, data);
    }

    @Override public final <T> T deserialize(Class<T> expectedType, byte[] data) {
        initializeIfNeeded();

        return doDeserialize(expectedType, data);
    }

    @Override public final <T> T deserialize(InputStream inputStream) {
        return (T) deserialize(Object.class, inputStream);
    }

    @Override public final <T> T deserialize(Class<T> expectedType, InputStream inputStream) {
        initializeIfNeeded();

        return doDeserialize(expectedType, inputStream);
    }

    /**
     * Called before any serialization or deserialization is done.
     * Allows the serializer to initialize.
     *
     * @param registrationRequired true if only registered classes are allowed.
     * @param registeredClasses the registered classes, sorted by classname.
     */
    protected abstract void initialize(boolean registrationRequired, List<Class> registeredClasses);

    /**
     * Serialize an object to a byte array.
     */
    protected abstract byte[] doSerialize(Object object);

    /**
     * Deserialize an object from a byte array.
     */
    protected abstract <T> T doDeserialize(Class<T> expectedType, byte[] data);

    /**
     * Deserialize an object from an input stream.
     */
    protected abstract <T> T doDeserialize(Class<T> expectedType, InputStream inputStream);

    private void initializeIfNeeded() {
        if (!initialized) {
            synchronized (initializationLock) {
                if (!initialized) {
                    // Order of classes is important for some serializers, so sort the registered classes by name to get an uniform order for the same set of classes.
                    List<Class> registeredClasses = new ArrayList<Class>(allowedClasses);
                    Collections.sort(registeredClasses, CLASS_NAME_COMPARATOR);
                    initialize(requireRegistration, registeredClasses);
                    initialized = true;
                }
            }
        }
    }

    private void checkNotInitialized() {
        if (initialized) throw new IllegalStateException("Can not modify the settings after the serializer has been used to serialize or deserialize something.");
    }
}
