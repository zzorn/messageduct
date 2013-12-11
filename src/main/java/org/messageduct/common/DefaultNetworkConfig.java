package org.messageduct.common;

import org.flowutils.Check;
import org.messageduct.account.messages.*;
import org.messageduct.utils.serializer.ConcurrentSerializer;
import org.messageduct.utils.serializer.ConcurrentSerializerWrapper;
import org.messageduct.utils.serializer.Serializer;

import java.util.*;

import static org.flowutils.Check.notNull;

/**
 * Class with default values and allowing primitive types as well as the messaging classes used by account management.
 *
 * It is recommended to override this to create a common NetworkConfig implementation that registers the allowed
 * classes, and then instantiate the class on both the client and server, to make sure that the same allowed classes
 * are used on both sides.
 */
public class DefaultNetworkConfig implements NetworkConfig {

    public static final int DEFAULT_PORT = 28866;
    public static final int DEFAULT_IDLE_TIME_SECONDS = 30;

    private int port;
    private boolean encryptionEnabled;
    private boolean compressionEnabled;
    private int idleTimeSeconds;
    private ConcurrentSerializer serializer;

    private final Set<Class> allowedClasses = new HashSet<Class>();

    /**
     * Creates a new DefaultNetworkConfig with default values and the specified allowed classes.
     *
     * @param allowedClasses zero or more classes that are allowed to be sent over the network.
     */
    public DefaultNetworkConfig(Class... allowedClasses) {
        this(DEFAULT_PORT, true, true, DEFAULT_IDLE_TIME_SECONDS, new ConcurrentSerializerWrapper(Serializer.class), Arrays.asList(allowedClasses));
    }


    /**
     * Creates a new configuration with the specified parameter values.
     *
     * @param port the port that the server listens to.
     * @param encryptionEnabled if true, the connection will be encrypted.
     * @param compressionEnabled if true, the connection will be compressed.
     * @param idleTimeSeconds number of seconds after which an idle event is triggered.
     * @param serializer a thread safe serializer that is used to serialize and deserialize messages sent over the network.
     * @param allowedClasses the classes that are allowed to be sent over the network.
     */

    public DefaultNetworkConfig(int port,
                                boolean encryptionEnabled,
                                boolean compressionEnabled,
                                int idleTimeSeconds,
                                ConcurrentSerializer serializer,
                                Collection<Class> allowedClasses) {
        this.port = port;
        this.encryptionEnabled = encryptionEnabled;
        this.compressionEnabled = compressionEnabled;
        setIdleTimeSeconds(idleTimeSeconds);
        this.serializer = serializer;
        registerAllowedClasses(allowedClasses);

        // Register classes allowed by default
        registerPrimitiveTypes();
        registerCollectionTypes();
        registerAccountManagementClasses();
    }

    @Override public int getPort() {
        return port;
    }

    @Override public void setPort(int port) {
        Check.positive(port, "port");
        this.port = port;
    }

    @Override public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    @Override public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    @Override public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    @Override public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    @Override public ConcurrentSerializer getSerializer() {
        // Make sure the allowed classes get registered with the serializer before it is used.
        if (serializer != null) {
            serializer.registerAllowedClasses(allowedClasses);
        }

        return serializer;
    }

    @Override public void setSerializer(ConcurrentSerializer serializer) {
        Check.notNull(serializer, "serializer");

        this.serializer = serializer;
    }

    @Override public int getIdleTimeSeconds() {
        return idleTimeSeconds;
    }

    @Override public void setIdleTimeSeconds(int idleTimeSeconds) {
        Check.positive(idleTimeSeconds, "idleTimeSeconds");

        this.idleTimeSeconds = idleTimeSeconds;
    }

    @Override public Set<Class> getAllowedClasses() {
        return serializer.getAllowedClasses();
    }

    @Override public void registerAllowedClass(Class allowedClass) {
        notNull(allowedClass, "allowedClass");

        allowedClasses.add(allowedClass);
    }

    @Override public void registerAllowedClasses(Class... classes) {
        for (Class aClass : classes) {
            registerAllowedClass(aClass);
        }
    }

    @Override public void registerAllowedClasses(Collection<Class> classes) {
        for (Class aClass : classes) {
            registerAllowedClass(aClass);
        }
    }

    protected void registerAccountManagementClasses() {
        registerAllowedClasses(LoginMessage.class,
                               LoginSuccessMessage.class,
                               CreateAccountMessage.class,
                               CreateAccountSuccessMessage.class,
                               ErrorMessage.class);

        /* TODO: Uncomment when implemented
        registerAllowedClasses(DeleteAccountRequestMessage.class,
                               DeleteAccountExecuteMessage.class,
                               DeleteAccountSuccessMessage.class,
                               PasswordResetRequestMessage.class,
                               PasswordResetExecuteMessage.class,
                               ChangePasswordMessage.class);
        */
    }

    protected void registerPrimitiveTypes() {
        registerAllowedClasses(Boolean.class,
                               Byte.class,
                               Short.class,
                               Integer.class,
                               Long.class,
                               Float.class,
                               Double.class,
                               Character.class,
                               String.class);

        registerAllowedClasses(Boolean.TYPE,
                               Byte.TYPE,
                               Short.TYPE,
                               Integer.TYPE,
                               Long.TYPE,
                               Float.TYPE,
                               Double.TYPE,
                               Character.TYPE);
    }

    protected void registerCollectionTypes() {
        registerAllowedClasses(ArrayList.class,
                               LinkedList.class,
                               HashMap.class,
                               HashSet.class);
    }
}
