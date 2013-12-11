package org.messageduct.utils.serializer;

/**
 * Interface for Serializers that are guaranteed to be thread safe,
 * that is, allows simultaneous serializing on different threads.
 */
public interface ConcurrentSerializer extends Serializer {
}
