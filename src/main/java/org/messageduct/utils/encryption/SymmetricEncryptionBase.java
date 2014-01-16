package org.messageduct.utils.encryption;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.security.SecureRandom;

import static org.flowutils.Check.equal;
import static org.flowutils.Check.notNull;

/**
 * Implements encryption and decryption of strings using the encryption and decryption of byte arrays.
 * Also implements a way to check whether a decryption password was correct.
 */
public abstract class SymmetricEncryptionBase implements SymmetricEncryption {

    private static final Charset CHARSET = Charset.forName("UTF8");

    /**
     * Prefix added to plaintext data when encrypting, to detect if the password is correct.
     */
    public static final byte[] DEFAULT_PASSWORD_VERIFICATION_PREFIX = "MsgDuctEncPre".getBytes(CHARSET);

    private final byte[] passwordVerificationPrefix;

    /**
     * Creates a EncryptionProviderBase with the default password verification prefix.
     */
    protected SymmetricEncryptionBase() {
        this(DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a EncryptionProviderBase with the specified password verification prefix.
     *
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     *                                   If null, no prefix is added, and no password verification is done.
     *                                   Make sure you use the same prefix when encoding and decoding data.
     */
    protected SymmetricEncryptionBase(byte[] passwordVerificationPrefix) {
        this.passwordVerificationPrefix = passwordVerificationPrefix;
    }

    @Override
    public byte[] generateNewRandomKey() {
        byte key[] = new byte[getKeyLengthBits() / 8];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }

    @Override
    public final byte[] encrypt(byte[] plaintextData, final byte[] key) {
        notNull(plaintextData, "plaintextData");
        notNull(key, "key");
        equal(key.length * 8, "Length of given key, in bits", getKeyLengthBits(), "Required key length, in bits");

        // Add password verification prefix if specified
        plaintextData = EncryptionUtils.addPasswordVerificationPrefix(plaintextData, passwordVerificationPrefix);

        // Encrypt data
        return doEncrypt(plaintextData, key);
    }

    @Override
    public final byte[] decrypt(final byte[] encryptedData, final byte[] key) throws WrongPasswordException {
        notNull(encryptedData, "encryptedData");
        notNull(key, "key");
        equal(key.length*8, "Length of given key, in bits", getKeyLengthBits(), "Required key length, in bits");

        // Decrypt data
        byte[] decryptedData = doDecrypt(encryptedData, key);

        // Verify the password and remove the verification prefix.
        decryptedData = EncryptionUtils.verifyPasswordVerificationPrefix(decryptedData, passwordVerificationPrefix, "password");

        return decryptedData;
    }

    @Override public final byte[] encrypt(byte[] plaintextData, char[] password) {
        return encrypt(plaintextData, generateKey(password));
    }

    @Override public final byte[] decrypt(byte[] encryptedData, char[] password) throws WrongPasswordException {
        return decrypt(encryptedData, generateKey(password));
    }

    @Override public final String encrypt(String plaintextString, char[] password) {
        notNull(plaintextString, "plaintextString");
        notNull(password, "password");

        // Get the text in binary form using UTF8 charset
        final byte[] plaintextData = plaintextString.getBytes(CHARSET);

        // Encrypt the binary form with the password
        final byte[] encryptedData = encrypt(plaintextData, password);

        // Convert the encrypted data to text using base64 encoding
        return Base64.encodeBase64String(encryptedData);
    }

    @Override public final String decrypt(String encryptedString, char[] password) throws WrongPasswordException {
        notNull(encryptedString, "encryptedString");
        notNull(password, "password");
        if (!Base64.isBase64(encryptedString)) throw new IllegalArgumentException("The provided encrypted string: \n"+encryptedString+"\n is not base64 encoded, can not decode.  Data corrupt?");

        // Convert base64 encoded encrypted data to binary data
        final byte[] encryptedData = Base64.decodeBase64(encryptedString);

        // Decrypt the binary data with the password
        final byte[] decryptedData = decrypt(encryptedData, password);

        // Create a string from the decrypted binary data in UTF8 format
        return new String(decryptedData, CHARSET);
    }


    /**
     * Do actual encryption.
     *
     * @param plaintextData data to encrypt
     * @param key to use.  Must be of correct length.
     * @return encrypted data.
     */
    protected abstract byte[] doEncrypt(byte[] plaintextData, byte[] key);

    /**
     * Do actual decryption.
     *
     * @param encryptedData data to decrypt
     * @param key to use.  Must be of correct length.
     * @return decrypted data.
     */
    protected abstract byte[] doDecrypt(byte[] encryptedData, byte[] key);

    /**
     * @param password password to use for generating the key.
     * @return a key of the correct length for the cipher used, based on the specified password.
     */
    protected abstract byte[] generateKey(char[] password);

}
