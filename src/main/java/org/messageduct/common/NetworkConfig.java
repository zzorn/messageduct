package org.messageduct.common;

import org.messageduct.utils.serializer.ConcurrentSerializer;

import java.util.Collection;
import java.util.Set;

/**
 * Application specific configuration for a connection between the server and client.
 *
 * Use by creating an implementing class that returns the desired application specific values, and instantiating it on
 * both the server and client to configure the networking, to ensure that both use the same settings.
 *
 * Any registering of allowed classes or changing of the other configuration settings should be done before the config
 * is passed to the networking service.
 *
 * NOTE: It's important that the server and client have exactly the same allowed classes!
 */
public interface NetworkConfig {

    /**
     * @return true if encryption should be used for the connection, false if not.
     */
    boolean isEncryptionEnabled();

    /**
     * Should not be called after the configuration has been passed to the networking service.
     * @param enableEncryption true if encryption should be used for the connection, false if not.
     */
    void setEncryptionEnabled(boolean enableEncryption);

    /**
     * @return true if compression should be used for the connection, false if not.
     */
    boolean isCompressionEnabled();

    /**
     * Should not be called after the configuration has been passed to the networking service.
     * @param enableCompression true if compression should be used for the connection, false if not.
     */
    void setCompressionEnabled(boolean enableCompression);

    /**
     * @return serializer to use when serializing message objects to network traffic and back.
     *         Automatically configured with the allowed classes when retrieved.
     */
    ConcurrentSerializer getSerializer();

    /**
     * @param serializer serializer to use when serializing message objects to network traffic and back.
     */
    void setSerializer(ConcurrentSerializer serializer);

    /**
     * @return the classes that are allowed to be sent over the network connection as messages or contained in messages.
     *         No other classes are allowed, and will raise an exception or error if attempted to send or receive.
     */
    Set<Class> getAllowedClasses();

    /**
     * Adds a class that will be allowed to be transferred over connections.
     * Should not be called after the configuration has been passed to the networking service.
     */
    void registerAllowedClass(Class aClass);

    /**
     * Adds a set of classes that will be allowed to be transferred over connections.
     * Should not be called after the configuration has been passed to the networking service.
     */
    void registerAllowedClasses(Class... classes);

    /**
     * Adds a set of classes that will be allowed to be transferred over connections.
     * Should not be called after the configuration has been passed to the networking service.
     */
    void registerAllowedClasses(Collection<Class> classes);

    /**
     * @return the port that the server listens to by default.
     */
    int getPort();

    /**
     * @param defaultPort the port that the server listens to.
     */
    void setPort(int defaultPort);

    /**
     * @return number of seconds after an idle event is triggered.
     */
    int getIdleTimeSeconds();

    /**
     * @param idleTimeSeconds number of seconds after an idle event is triggered.
     */
    void setIdleTimeSeconds(int idleTimeSeconds);

    /**
     * @return if true, messages sent or received over the connection will be logged.  Defaults to false.
     */
    boolean isMessageLoggingEnabled();

    /**
     * @param messageLoggingEnabled if true, messages sent or received over the connection will be logged.
     */
    void setMessageLoggingEnabled(boolean messageLoggingEnabled);
}
