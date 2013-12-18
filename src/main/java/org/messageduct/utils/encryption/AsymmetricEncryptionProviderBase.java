package org.messageduct.utils.encryption;

import org.apache.commons.codec.binary.Base64;
import org.flowutils.Check;
import org.messageduct.utils.ByteArrayUtils;
import org.messageduct.utils.StreamUtils;
import org.messageduct.utils.serializer.KryoSerializer;
import org.messageduct.utils.serializer.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Set;

import static org.flowutils.Check.notNull;

/**
 * Common functionality for asymmetric encryption.
 */
public abstract class AsymmetricEncryptionProviderBase implements AsymmetricEncryptionProvider {

    private static final Charset CHARSET = Charset.forName("UTF8");

    /**
     * Prefix added to plaintext data when encrypting, to detect if the password is correct.
     */
    public static final byte[] DEFAULT_PASSWORD_VERIFICATION_PREFIX = "EncryptedByAEPB".getBytes(CHARSET);

    private final byte[] passwordVerificationPrefix;

    private final Serializer serializer;

    /**
     * Creates a AsymmetricEncryptionProviderBase with the default password verification prefix.
     *
     * @param allowedPublicKeySerializationClasses the classes that are expected to be contained in the public key.
     *                                             Used to restrict public key deserialization.
     */
    protected AsymmetricEncryptionProviderBase(Collection<Class> allowedPublicKeySerializationClasses) {
        this(allowedPublicKeySerializationClasses, DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a AsymmetricEncryptionProviderBase with the specified password verification prefix.
     *
     * @param allowedPublicKeySerializationClasses the classes that are expected to be contained in the public key.
     *                                             Used to restrict public key deserialization.
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     *                                   If null, no prefix is added, and no password verification is done.
     *                                   Make sure you use the same prefix when encoding and decoding data.
     */
    protected AsymmetricEncryptionProviderBase(Collection<Class> allowedPublicKeySerializationClasses, byte[] passwordVerificationPrefix) {
        this(new KryoSerializer(allowedPublicKeySerializationClasses), passwordVerificationPrefix);
    }

    /**
     * Creates a AsymmetricEncryptionProviderBase with the specified password verification prefix.
     *
     * @param serializer the serializer to use for serializing the public key.
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     *                                   If null, no prefix is added, and no password verification is done.
     *                                   Make sure you use the same prefix when encoding and decoding data.
     */
    protected AsymmetricEncryptionProviderBase(Serializer serializer, byte[] passwordVerificationPrefix) {
        Check.notNull(serializer, "serializer");

        this.serializer = serializer;
        this.passwordVerificationPrefix = passwordVerificationPrefix;
    }

    @Override public void serializePublicKey(PublicKey publicKey, OutputStream outputStream) throws IOException {
        outputStream.write(serializer.serialize(publicKey));
    }

    @Override public PublicKey deserializePublicKey(InputStream inputStream) throws IOException {
        return serializer.deserialize(inputStream);
    }

    @Override public final byte[] encrypt(byte[] dataToEncrypt, PublicKey publicKey) {
        // Add password verification prefix if specified
        dataToEncrypt = EncryptionUtils.addPasswordVerificationPrefix(dataToEncrypt, passwordVerificationPrefix);

        return doEncrypt(publicKey, dataToEncrypt);
    }

    @Override public final byte[] decrypt(byte[] dataToDecrypt, PrivateKey privateKey) throws WrongPasswordException {
        byte[] decryptedData = doDecrypt(privateKey, dataToDecrypt);

        // Verify the password and remove the verification prefix.
        decryptedData = EncryptionUtils.verifyPasswordVerificationPrefix(decryptedData, passwordVerificationPrefix, "private key");

        return decryptedData;
    }

    @Override public final byte[] encryptCharacters(char[] dataToEncrypt, PublicKey publicKey) {
        return encrypt(String.valueOf(dataToEncrypt).getBytes(CHARSET), publicKey);
    }

    @Override public final char[] decryptCharacters(byte[] dataToDecrypt, PrivateKey privateKey) throws WrongPasswordException {
        return new String(decrypt(dataToDecrypt, privateKey), CHARSET).toCharArray();
    }

    @Override public final String encryptString(String plaintextString, PublicKey publicKey) {
        notNull(plaintextString, "plaintextString");
        Check.notNull(publicKey, "publicKey");

        // Get the text in binary form using UTF8 charset
        final byte[] plaintextData = plaintextString.getBytes(CHARSET);

        // Encrypt the binary form with the password
        final byte[] encryptedData = encrypt(plaintextData, publicKey);

        // Convert the encrypted data to text using base64 encoding
        return Base64.encodeBase64String(encryptedData);
    }

    @Override public final String decryptString(String encryptedString, PrivateKey privateKey) throws WrongPasswordException {
        notNull(encryptedString, "encryptedString");
        Check.notNull(privateKey, "privateKey");
        if (!Base64.isBase64(encryptedString)) throw new IllegalArgumentException("The provided encrypted string: \n"+encryptedString+"\n is not base64 encoded, can not decode.  Data corrupt?");

        // Convert base64 encoded encrypted data to binary data
        final byte[] encryptedData = Base64.decodeBase64(encryptedString);

        // Decrypt the binary data with the password
        final byte[] decryptedData = decrypt(encryptedData, privateKey);

        // Create a string from the decrypted binary data in UTF8 format
        return new String(decryptedData, CHARSET);
    }


    protected abstract byte[] doEncrypt(PublicKey publicKey, byte[] dataToEncrypt);

    protected abstract byte[] doDecrypt(PrivateKey privateKey, byte[] dataToDecrypt);

}
