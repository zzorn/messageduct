package org.messageduct.utils.encryption;

import org.apache.commons.codec.binary.Base64;
import org.flowutils.Check;
import org.messageduct.utils.ByteArrayUtils;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 * Implements encryption and decryption of strings using the encryption and decryption of byte arrays.
 * Also implements a way to check whether a decryption password was correct.
 */
public abstract class EncryptionProviderBase implements EncryptionProvider {

    private static final Charset CHARSET = Charset.forName("UTF8");

    /**
     * Prefix added to plaintext data when encrypting, to detect if the password is correct.
     */
    public static final byte[] DEFAULT_PASSWORD_VERIFICATION_PREFIX = "EncryptedByCEP".getBytes(CHARSET);

    private final byte[] passwordVerificationPrefix;

    /**
     * Creates a EncryptionProviderBase with the default password verification prefix.
     */
    protected EncryptionProviderBase() {
        this(DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a EncryptionProviderBase with the specified password verification prefix.
     *
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     *                                   If null, no prefix is added, and no password verification is done.
     *                                   Make sure you use the same prefix when encoding and decoding data.
     *
     */
    protected EncryptionProviderBase(byte[] passwordVerificationPrefix) {
        this.passwordVerificationPrefix = passwordVerificationPrefix;
    }

    @Override public final byte[] encrypt(byte[] plaintextData, char[] password) {
        notNull(plaintextData, "plaintextData");
        notNull(password, "password");

        // Add password verification prefix if specified
        if (passwordVerificationPrefix != null) {
            plaintextData = ByteArrayUtils.concatenateByteArrays(passwordVerificationPrefix, plaintextData);
        }

        // Encrypt data
        return doEncrypt(plaintextData, password);
    }

    @Override public final byte[] decrypt(byte[] encryptedData, char[] password) throws WrongPasswordException {
        notNull(encryptedData, "encryptedData");
        notNull(password, "password");

        // Decrypt data
        byte[] decryptedData = doDecrypt(encryptedData, password);

        // Check password verification prefix if specified
        if (passwordVerificationPrefix != null) {
            // Check length
            if (decryptedData.length < passwordVerificationPrefix.length) throw new WrongPasswordException("Wrong password or corrupted data, decrypted data too short for password verification string.");

            // Check prefix
            for (int i = 0; i < passwordVerificationPrefix.length; i++) {
                if (decryptedData[i] != passwordVerificationPrefix[i]) throw new WrongPasswordException("Wrong password or corrupted data, password verification string mismatch at character number "+i+"");
            }

            // Remove the prefix from the data
            decryptedData = ByteArrayUtils.removeArrayPrefix(decryptedData, passwordVerificationPrefix.length);
        }

        return decryptedData;
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
     * @param password password to use
     * @return encrypted data.
     */
    protected abstract byte[] doEncrypt(byte[] plaintextData, char[] password);

    /**
     * Do actual decryption.
     *
     * @param encryptedData data to decrypt
     * @param password password to use
     * @return decrypted data.
     */
    protected abstract byte[] doDecrypt(byte[] encryptedData, char[] password);


}
