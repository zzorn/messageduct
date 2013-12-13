package org.messageduct.utils.storage;

import java.io.IOException;

/**
 * Storage base class that handles synchronizing.
 */
public abstract class SynchronizedStorage implements Storage {

    private final Object lock = new Object();

    @Override public final void save(Object object) throws IOException {
        synchronized (lock) {
            doSave(object);
        }
    }

    @Override public final <T> T load() throws IOException {
        final T object;
        synchronized (lock) {
            object = doLoad();
        }
        return object;
    }

    /**
     * Save object.
     *
     * Will only be called from one thread at a time, will not be called at the same time as doLoad.
     */
    protected abstract void doSave(Object object) throws IOException;

    /**
     * Load object.
     *
     * Will only be called from one thread at a time, will not be called at the same time as doSave.
     */
    protected abstract <T> T doLoad() throws IOException;
}
