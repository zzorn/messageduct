package org.messageduct.utils.service;

/**
 * Something that can be initialized and shut down.
 */
public interface Service {

    /**
     * Initialize the service, do any required startup tasks, reserve resources, etc.
     * Must be called before the Service is used.
     */
    void init();

    /**
     * Shuts down the service, frees any resources.
     * Must be called before the application closes.
     * After the Service has been shut down, it can not be initialized again.
     */
    void shutdown();

    /**
     * @return name of the service, for use in logging and error messages.
     */
    String getServiceName();

    /**
     * @return true if the Service is active (initialized but not yet shutdown.
     */
    boolean isActive();

    /**
     * @return true if the service has ever been initialized (it might have been shutdown as well though).
     */
    boolean isInitialized();

    /**
     * @return true if the service has been shutdown.
     */
    boolean isShutdown();
}
