package org.messageduct.utils.service;

import org.messageduct.utils.ThreadUtils;

/**
 * Base class that provides general Service lifecycle related functionality.
 */
// TODO: Add logging
// TODO: Add option for shutdown hook upon VM exit?
public abstract class ServiceBase implements Service {

    private boolean initialized = false;
    private boolean shutdown = false;

    @Override public String getServiceName() {
        return getClass().getSimpleName();
    }

    @Override public final void init() {
        if (initialized) {
            throw new IllegalStateException("The Service " + getServiceName() + " has already been initialized, can not initialize again!");
        }

        doInit();

        initialized = true;
    }

    @Override public final void shutdown() {
        if (shutdown) {
            throw new IllegalStateException("The Service " + getServiceName() + " has already been shutdown, can not shutdown again!");
        }

        // Only do the shutdown if we were initialized earlier
        if (initialized) {
            doShutdown();
        }

        shutdown = true;
    }

    @Override public final boolean isActive() {
        return initialized && !shutdown;
    }

    @Override public final boolean isInitialized() {
        return initialized;
    }

    @Override public final boolean isShutdown() {
        return shutdown;
    }

    /**
     * Do the initialization.
     */
    protected abstract void doInit();

    /**
     * Do the shutdown.
     */
    protected abstract void doShutdown();

    /**
     * Utility method that ensures that the service is active, and throws an exception if that is not the case.
     */
    protected final void ensureActive() {
        ensureActive(null);
    }

    /**
     * Utility method that ensures that the service is active, and throws an exception if that is not the case.
     * @param action description of the action we were about to take.  Included in error message if the service is not active.
     */
    protected final void ensureActive(String action) {
        if (!initialized) {
            if (action == null) action = "invoke " + ThreadUtils.getNameOfCallingMethod();
            throw new IllegalStateException("Can not " + action + ", the " + getServiceName() + " service has not yet been initialized!  Call init first.");
        }
        if (shutdown) {
            if (action == null) action = "invoke " + ThreadUtils.getNameOfCallingMethod();
            throw new IllegalStateException("Can not " + action + ", the " + getServiceName() + " service has already been shut down!");
        }
    }

}
