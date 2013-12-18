package org.messageduct.utils.serializer;

/**
 * Factory for creating serializers.
 */
public interface SerializerFactory {

    /**
     * @return a new serializer, with no data or buffers shared with other serializers.
     */
    Serializer createSerializer();

}
