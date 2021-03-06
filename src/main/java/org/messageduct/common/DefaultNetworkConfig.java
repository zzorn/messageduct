package org.messageduct.common;

import org.flowutils.Check;
import org.flowutils.Symbol;
import org.messageduct.account.messages.*;
import org.messageduct.serverinfo.DefaultServerInfo;
import org.messageduct.serverinfo.ServerInfoMessage;
import org.messageduct.serverinfo.ServerInfoRequestMessage;
import java.security.KeyPair;
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
    public static final int DEFAULT_MESSAGE_SIZE = 1024*1024;

    private int port;
    private boolean encryptionEnabled;
    private boolean compressionEnabled;
    private boolean messageLoggingEnabled;
    private int idleTimeSeconds;
    private int maximumMessageSize;
    private KeyPair serverKeys;

    private final Set<Class> allowedClasses = new LinkedHashSet<Class>();

    /**
     * Creates a new DefaultNetworkConfig with default values and the specified allowed classes.
     *
     * @param allowedClasses zero or more classes that are allowed to be sent over the network.
     */
    public DefaultNetworkConfig(Class... allowedClasses) {
        this(DEFAULT_PORT,
             true,
             true,
             false,
             DEFAULT_IDLE_TIME_SECONDS,
             DEFAULT_MESSAGE_SIZE,
             null,
             Arrays.asList(allowedClasses));
    }


    /**
     * Creates a new configuration with the specified parameter values.
     *
     * @param port the port that the server listens to.
     * @param encryptionEnabled if true, the connection will be encrypted.
     * @param compressionEnabled if true, the connection will be compressed.
     * @param messageLoggingEnabled if true, messages sent or received over the connection will be logged.  Defaults to false.
     * @param idleTimeSeconds number of seconds after which an idle event is triggered.
     * @param maximumMessageSize maximum size of a message in bytes (serialized, packed, or encrypted sizes all have to be smaller than this).
     * @param allowedClasses the classes that are allowed to be sent over the network.
     */

    public DefaultNetworkConfig(int port,
                                boolean encryptionEnabled,
                                boolean compressionEnabled,
                                boolean messageLoggingEnabled,
                                int idleTimeSeconds,
                                int maximumMessageSize,
                                Collection<Class> allowedClasses) {

        this(port,
             encryptionEnabled,
             compressionEnabled,
             messageLoggingEnabled,
             idleTimeSeconds,
             maximumMessageSize,
             null,
             allowedClasses);
    }

    /**
     * Creates a new configuration with the specified parameter values.
     *
     * @param port the port that the server listens to.
     * @param encryptionEnabled if true, the connection will be encrypted.
     * @param compressionEnabled if true, the connection will be compressed.
     * @param messageLoggingEnabled if true, messages sent or received over the connection will be logged.  Defaults to false.
     * @param idleTimeSeconds number of seconds after which an idle event is triggered.
     * @param maximumMessageSize maximum size of a message in bytes (serialized, packed, or encrypted sizes all have to be smaller than this).
     * @param serverKeys keypair used by the server to identify itself to the clients.  Pass in null on the client.
     * @param allowedClasses the classes that are allowed to be sent over the network.
     */

    public DefaultNetworkConfig(int port,
                                boolean encryptionEnabled,
                                boolean compressionEnabled,
                                boolean messageLoggingEnabled,
                                int idleTimeSeconds,
                                int maximumMessageSize,
                                KeyPair serverKeys,
                                Collection<Class> allowedClasses) {
        this.port = port;
        this.encryptionEnabled = encryptionEnabled;
        this.compressionEnabled = compressionEnabled;
        this.messageLoggingEnabled = messageLoggingEnabled;
        this.serverKeys = serverKeys;
        setIdleTimeSeconds(idleTimeSeconds);
        setMaximumMessageSize(maximumMessageSize);

        registerDefaultAllowedClasses();

        registerAllowedClasses(allowedClasses);
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

    @Override public boolean isMessageLoggingEnabled() {
        return messageLoggingEnabled;
    }

    @Override public void setMessageLoggingEnabled(boolean messageLoggingEnabled) {
        this.messageLoggingEnabled = messageLoggingEnabled;
    }

    @Override public int getMaximumMessageSize() {
        return maximumMessageSize;
    }

    @Override public void setMaximumMessageSize(int sizeInBytes) {
        Check.positive(sizeInBytes, "sizeInBytes");
        maximumMessageSize = sizeInBytes;
    }

    @Override public int getIdleTimeSeconds() {
        return idleTimeSeconds;
    }

    @Override public void setIdleTimeSeconds(int idleTimeSeconds) {
        Check.positive(idleTimeSeconds, "idleTimeSeconds");

        this.idleTimeSeconds = idleTimeSeconds;
    }

    @Override public KeyPair getServerKeys() {
        return serverKeys;
    }

    @Override public void setServerKeys(KeyPair serverKeys) {
        this.serverKeys = serverKeys;
    }

    @Override public Set<Class> getAllowedClasses() {
        return allowedClasses;
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

    protected void registerDefaultAllowedClasses() {
        registerPrimitiveTypes();
        registerCollectionTypes();
        registerAccountManagementClasses();
        registerServerInfoClasses();
        registerAllowedClasses(String.class);
        registerAllowedClasses(Symbol.class);
    }

    protected void registerPrimitiveTypes() {
        registerAllowedClasses(char[].class,
                               boolean[].class,
                               byte[].class,
                               short[].class,
                               int[].class,
                               long[].class,
                               float[].class,
                               double[].class);
        registerAllowedClasses(Byte.class,
                               Short.class,
                               Integer.class,
                               Long.class,
                               Float.class,
                               Double.class,
                               Character.class);
    }

    protected void registerCollectionTypes() {
        registerAllowedClasses(ArrayList.class,
                               LinkedList.class,
                               HashMap.class,
                               HashSet.class);
    }

    protected void registerAccountManagementClasses() {
        registerAllowedClasses(LoginMessage.class,
                               LoginSuccessMessage.class,
                               CreateAccountMessage.class,
                               CreateAccountSuccessMessage.class,
                               AccountErrorMessage.class);

        /* TODO: Uncomment when implemented
        registerAllowedClasses(DeleteAccountRequestMessage.class,
                               DeleteAccountExecuteMessage.class,
                               DeleteAccountSuccessMessage.class,
                               PasswordResetRequestMessage.class,
                               PasswordResetExecuteMessage.class,
                               ChangePasswordMessage.class);
        */
    }

    private void registerServerInfoClasses() {
        registerAllowedClasses(ServerInfoRequestMessage.class,
                               ServerInfoMessage.class,
                               DefaultServerInfo.class);
    }
}
