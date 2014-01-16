package org.messageduct.utils.encryption;

import org.apache.commons.codec.binary.Base64;
import org.flowutils.Check;
import org.messageduct.utils.SecurityUtils;
import org.messageduct.utils.StreamUtils;
import org.messageduct.utils.serializer.KryoSerializer;
import org.messageduct.utils.serializer.Serializer;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;

import static org.flowutils.Check.equal;
import static org.flowutils.Check.notNull;

/**
 * Implements encryption and decryption of strings using the encryption and decryption of byte arrays.
 * Also implements a way to check whether a decryption password was correct.
 */
public abstract class SymmetricEncryptionBase implements SymmetricEncryption {

    private static final Charset CHARSET = Charset.forName("UTF8");

    private static final int RANDOM_PASSWORD_LENGTH = 256;

    /**
     * Prefix added to plaintext data when encrypting, to detect if the password is correct.
     */
    public static final byte[] DEFAULT_PASSWORD_VERIFICATION_PREFIX = "MsgDuctEncPre".getBytes(CHARSET);

    private static final int MAX_SERIALIZED_SECRET_KEY_LENGTH = 10*1024;

    private final byte[] passwordVerificationPrefix;

    private final Serializer serializer;


    /**
     * Creates a EncryptionProviderBase with the default password verification prefix.
     */
    protected SymmetricEncryptionBase(Collection<Class> allowedSecretKeySerializationClasses) {
        this(allowedSecretKeySerializationClasses, DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a EncryptionProviderBase with the specified password verification prefix.
     *
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     *                                   If null, no prefix is added, and no password verification is done.
     *                                   Make sure you use the same prefix when encoding and decoding data.
     */
    protected SymmetricEncryptionBase(Collection<Class> allowedSecretKeySerializationClasses, byte[] passwordVerificationPrefix) {
        this.passwordVerificationPrefix = passwordVerificationPrefix;

        serializer = new KryoSerializer(allowedSecretKeySerializationClasses);
    }

    @Override
    public final byte[] encrypt(byte[] plaintextData, final SecretKey key) {
        notNull(plaintextData, "plaintextData");
        notNull(key, "key");

        // Add password verification prefix if specified
        plaintextData = EncryptionUtils.addPasswordVerificationPrefix(plaintextData, passwordVerificationPrefix);

        // Encrypt data
        return doEncrypt(plaintextData, key);
    }

    @Override
    public final byte[] decrypt(final byte[] encryptedData, final SecretKey key) throws WrongPasswordException {
        notNull(encryptedData, "encryptedData");
        notNull(key, "key");

        // Decrypt data
        byte[] decryptedData = doDecrypt(encryptedData, key);

        // Verify the password and remove the verification prefix.
        decryptedData = EncryptionUtils.verifyPasswordVerificationPrefix(decryptedData, passwordVerificationPrefix, "password");

        return decryptedData;
    }

    @Override public final byte[] encrypt(byte[] plaintextData, char[] password) {
        return encrypt(plaintextData, generateSecretKeyFromPassword(password));
    }

    @Override public final byte[] decrypt(byte[] encryptedData, char[] password) throws WrongPasswordException {
        return decrypt(encryptedData, generateSecretKeyFromPassword(password));
    }

    @Override public final String encrypt(String plaintextString, char[] password) {
        return encrypt(plaintextString, generateSecretKeyFromPassword(password));
    }

    @Override public final String decrypt(String encryptedString, char[] password) throws WrongPasswordException {
        return decrypt(encryptedString, generateSecretKeyFromPassword(password));
    }

    @Override public String encrypt(String plaintextString, SecretKey key) {
        notNull(plaintextString, "plaintextString");
        notNull(key, "key");

        // Get the text in binary form using UTF8 charset
        final byte[] plaintextData = plaintextString.getBytes(CHARSET);

        // Encrypt the binary form with the password
        final byte[] encryptedData = encrypt(plaintextData, key);

        // Convert the encrypted data to text using base64 encoding
        return Base64.encodeBase64String(encryptedData);
    }

    @Override public String decrypt(String encryptedString, SecretKey key) throws WrongPasswordException {
        notNull(encryptedString, "encryptedString");
        notNull(key, "key");
        if (!Base64.isBase64(encryptedString)) throw new IllegalArgumentException("The provided encrypted string: \n"+encryptedString+"\n is not base64 encoded, can not decode.  Data corrupt?");

        // Convert base64 encoded encrypted data to binary data
        final byte[] encryptedData = Base64.decodeBase64(encryptedString);

        // Decrypt the binary data with the password
        final byte[] decryptedData = decrypt(encryptedData, key);

        // Create a string from the decrypted binary data in UTF8 format
        return new String(decryptedData, CHARSET);
    }

    @Override
    public SecretKey generateSecretKeyRandomly() {
        // Create random password
        final char[] randomPassword = SecurityUtils.randomAsciiChars(RANDOM_PASSWORD_LENGTH);

        // Generate key by hashing it
        return generateSecretKeyFromPassword(randomPassword);
    }

    @Override public byte[] serializeSecretKey(SecretKey key) {
        Check.notNull(key, "key");

        return serializer.serialize(key);
    }

    @Override public SecretKey deserializeSecretKey(byte[] serializedKey) {
        Check.notNull(serializedKey, "serializedKey");

        return serializer.deserialize(serializedKey);
    }

    @Override public void serializeSecretKey(SecretKey key, OutputStream outputStream) throws IOException {
        Check.notNull(key, "key");
        Check.notNull(outputStream, "outputStream");

        // Serialize key
        final byte[] serializedKey = serializeSecretKey(key);

        // Write serialized key to stream
        StreamUtils.writeByteArray(outputStream, serializedKey);
    }

    @Override public SecretKey deserializeSecretKey(InputStream inputStream) throws IOException {
        Check.notNull(inputStream, "inputStream");

        // Read serialized key from stream
        final byte[] serializedKey = StreamUtils.readByteArray(inputStream, MAX_SERIALIZED_SECRET_KEY_LENGTH);

        // Deserialize key
        return deserializeSecretKey(serializedKey);
    }


    /**
     * Do actual encryption.
     *
     * @param plaintextData data to encrypt
     * @param key to use.  Must be of correct length.
     * @return encrypted data.
     */
    protected abstract byte[] doEncrypt(byte[] plaintextData, SecretKey key);

    /**
     * Do actual decryption.
     *
     * @param encryptedData data to decrypt
     * @param key to use for decryption.
     * @return decrypted data.
     */
    protected abstract byte[] doDecrypt(byte[] encryptedData, SecretKey key);

}
