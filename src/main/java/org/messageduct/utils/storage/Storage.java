package org.messageduct.utils.storage;

import java.io.IOException;

/**
 * Interface for something that stores a java object persistently.
 */
public interface Storage {

    /**
     * Store object.
     *
     * @param object object to store.
     * @throws IOException thrown if there was some problem when storing the object.
     */
    void save(Object object) throws IOException;

    /**
     * @return loaded object.
     */
    <T> T load() throws IOException;

}
