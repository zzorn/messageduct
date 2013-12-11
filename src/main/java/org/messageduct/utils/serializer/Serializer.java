package org.messageduct.utils.serializer;

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Serializes java objects to binary data and the other way.
 *
 * By default serializes only the classes that have been registered, and throws an exception if any other classes
 * are attempted to be serialized.
 *
 * A serializer implementation is not necessarily thread safe, use a ConcurrentSerializer implementation if you need
 * to use the serializer in a multithreaded environment.
 */
public interface Serializer {

    /**
     * @return if true, only the registered classes can be serialized,
     *         if false, any class can be serialized.  Defaults to true.
     */
    boolean isRequireRegistration();

    /**
     * @param allowAllClasses if true, only the registered classes can be serialized,
     *                        if false, any class can be serialized.  Defaults to true.
     */
    void setRequireRegistration(boolean allowAllClasses);

    /**
     * Must be called before the first call to serialize or deserialize.  Should not be called afterward.
     * @param allowedClasses add classes that are allowed to be serialized by this serializer.
     */
    void registerAllowedClasses(Collection<Class> allowedClasses);

    /**
     * Must be called before the first call to serialize or deserialize.  Should not be called afterward.
     * @param allowedClass class to add that is allowed to be serialized by this serializer.
     */
    void registerAllowedClass(Class allowedClass);

    /**
     * Must be called before the first call to serialize or deserialize.  Should not be called afterward.
     * @param allowedClasses add classes that are allowed to be serialized by this serializer.
     */
    void registerAllowedClasses(Class ... allowedClasses);

    /**
     *
     * @return the classes that are allowed to be serialized, if isRequireRegistration is false.
     */
    Set<Class> getAllowedClasses();

    /**
     * Serialize the object to a byte array.
     * @param object object to serialize.
     * @return serialized form of object.
     */
    byte[] serialize(Object object);

    /**
     * Deserialize the object from a byte array.
     *
     * @param data serialized form of object.
     * @return de-serialized object.
     */
    <T> T deserialize(byte[] data);

    /**
     * Deserialize the object from an input stream.
     *
     * @param inputStream input data as a stream.
     * @return de-serialized object.
     */
    <T> T deserialize(InputStream inputStream);

    /**
     * Deserialize the object from a byte array.
     *
     * @param expectedType expected type of object.  If unknown, just pass in Object.class.
     * @param data serialized form of object.
     * @return de-serialized object.
     */
    <T> T deserialize(Class<T> expectedType, byte[] data);

    /**
     * Deserialize the object from an input stream.
     *
     * @param expectedType expected type of object.  If unknown, just pass in Object.class.
     * @param inputStream input data as a stream.
     * @return de-serialized object.
     */
    <T> T deserialize(Class<T> expectedType, InputStream inputStream);

}
