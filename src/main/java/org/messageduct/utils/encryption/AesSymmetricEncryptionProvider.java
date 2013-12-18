package org.messageduct.utils.encryption;


import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.*;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

import static org.flowutils.Check.*;

/**
 * Implementation of EncryptionProvider that uses the Java cryptography API.
 */
public final class AesSymmetricEncryptionProvider extends SymmetricEncryptionProviderBase {

    static {
        EncryptionUtils.installBouncyCastleProviderIfNotInstalled();
    }


    /**
     * Just a randomly picked SALT.
     */
    private static final byte[] DEFAULT_SALT = {(byte) 0xF2, (byte) 0x85, (byte) 0x93, (byte) 0x18,
                                                (byte) 0x48, (byte) 0xC9, (byte) 0xA4, (byte) 0x06 };

    /**
     * Initialization vector for AES.  Some random values from random.org.
     */
    private static final byte[] DEFAULT_INITIALIZATION_VECTOR = new byte[] {
        (byte)0x0D, (byte)0x96, (byte)0x98, (byte)0x0E,
        (byte)0xDA, (byte)0x00, (byte)0x12, (byte)0xD3,
        (byte)0x99, (byte)0x10, (byte)0x25, (byte)0x8D,
        (byte)0xBD, (byte)0x7C, (byte)0xEB, (byte)0xE4 };

    private static final String KEY_HASH_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
    private static final String PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "AES";


    private final byte[] salt;
    private final byte[] initializationVector;

    /**
     * Creates a cipher based encryption provider using a default salt and password verification sequence.
     */
    public AesSymmetricEncryptionProvider() {
        this(DEFAULT_SALT);
    }

    /**
     * Creates a cipher based encryption provider with a default password verification sequence.
     *
     * @param salt salt to mix with the password.
     */
    public AesSymmetricEncryptionProvider(byte[] salt) {
        this(salt, DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a cipher based encryption provider.
     *
     * @param salt salt to mix with the password.
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     */
    public AesSymmetricEncryptionProvider(byte[] salt, byte[] passwordVerificationPrefix) {
        super(passwordVerificationPrefix);
        notNull(salt, "salt");

        this.salt = salt;

        initializationVector = Arrays.copyOf(DEFAULT_INITIALIZATION_VECTOR, DEFAULT_INITIALIZATION_VECTOR.length);
    }

    @Override protected byte[] doEncrypt(byte[] plaintextData, char[] password) {
        // Create cipher to encrypt with
        final BufferedBlockCipher cipher = createCipher(true, password);

        // Do encryption
        return process(plaintextData, cipher, "encrypting");
    }

    @Override protected byte[] doDecrypt(byte[] encryptedData, char[] password) {
        // Create cipher to decrypt with
        final BufferedBlockCipher cipher = createCipher(false, password);

        // Do decryption
        return process(encryptedData, cipher, "decrypting");
    }

    private byte[] process(byte[] data, BufferedBlockCipher cipher, String activityDescription) {
        try {
            // Create a temporary buffer to decode into
            byte[] buffer = new byte[cipher.getOutputSize(data.length)];

            // Process
            int bytesWritten = cipher.processBytes(data, 0, data.length, buffer, 0);
            bytesWritten += cipher.doFinal(buffer, bytesWritten);

            // Only keep the actual bytes written
            return Arrays.copyOf(buffer, bytesWritten);
        } catch (Exception e) {
            throw new IllegalStateException("Problem when "+activityDescription+" data: " + e.getMessage(), e);
        }
    }


    private BufferedBlockCipher createCipher(boolean encrypt, char[] password) {
        // Generate AES key from password and salt
        byte[] key = generateKey(password);

        try {

            // Setup cipher parameters with key and initialization vector
            final byte[] iv = Arrays.copyOf(initializationVector, initializationVector.length);
            CipherParameters params = new ParametersWithIV(new KeyParameter(key), iv);

            // Setup cipher with padding
            //BlockCipherPadding padding = new PKCS7Padding(); // This keeps failing with "pad block corrupted", using TBCPadding for now
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new TBCPadding());
            cipher.reset();

            cipher.init(encrypt, params);

            return cipher;
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem when creating cipher: " + e.getMessage(), e);
        }
    }

    private byte[] generateKey(char[] password) {
        try {
            // Generate raw AES key from password and salt
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 50, 256);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_HASH_ALGORITHM, PROVIDER);
            SecretKeySpec secretKey = new SecretKeySpec(keyFactory.generateSecret(pbeKeySpec).getEncoded(), KEY_ALGORITHM);
            return secretKey.getEncoded();
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem when generating key from password: " + e.getMessage(), e);
        }
    }



}
