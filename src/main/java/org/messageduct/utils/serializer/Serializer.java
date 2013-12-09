package org.messageduct.utils.serializer;

/**
 * Serializes java objects to binary data and the other way.
 */
public interface Serializer {

    byte[] serialize(Object object);

    <T> T deserialize(Class<T> expectedType, byte[] data);

}
