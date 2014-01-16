package org.messageduct.utils.encryption;


import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.TBCPadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.messageduct.utils.ByteArrayUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static org.flowutils.Check.notNull;

/**
 * Implementation of EncryptionProvider that uses the Java cryptography API.
 */
public final class AesEncryption extends SymmetricEncryptionBase {

    static {
        EncryptionUtils.installBouncyCastleProviderIfNotInstalled();
    }


    /**
     * Just a randomly picked SALT.
     */
    private static final byte[] DEFAULT_SALT = {(byte) 0xF2, (byte) 0x85, (byte) 0x93, (byte) 0x18,
                                                (byte) 0x48, (byte) 0xC9, (byte) 0xA4, (byte) 0x06};

    private static final String KEY_HASH_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
    private static final String PROVIDER           = "BC"; // Use bouncy castle version of algorithms
    private static final String KEY_ALGORITHM      = "AES";
    private static final int    KEY_LENGTH_BITS    = 256;
    private static final int    KEY_HASH_ROUNDS    = 50;
    private static final int    BLOCK_LENGTH_BITS  = 128;
    private static final int    BLOCK_LENGTH_BYTES = BLOCK_LENGTH_BITS / 8;

    private final SecureRandom secureRandom;
    private final byte[] salt;

    /**
     * Creates a cipher based encryption provider using a default salt and password verification sequence.
     */
    public AesEncryption() {
        this(DEFAULT_SALT);
    }

    /**
     * Creates a cipher based encryption provider with a default password verification sequence.
     *
     * @param salt salt to mix with the password.
     */
    public AesEncryption(byte[] salt) {
        this(salt, DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    /**
     * Creates a cipher based encryption provider.
     *
     * @param salt salt to mix with the password.
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     */
    public AesEncryption(byte[] salt, byte[] passwordVerificationPrefix) {
        this(salt, passwordVerificationPrefix, new SecureRandom());
    }

    /**
     * Creates a cipher based encryption provider.
     *
     * @param salt salt to mix with the password.
     * @param passwordVerificationPrefix a fixed sequence that is added to the plaintext before encrypting,
     *                                   to make it possible to check whether decryption was successful.
     * @param secureRandom random number generator used for generating initialization vectors.
     */
    public AesEncryption(final byte[] salt, final byte[] passwordVerificationPrefix, SecureRandom secureRandom) {
        super(passwordVerificationPrefix);
        notNull(salt, "salt");
        notNull(secureRandom, "secureRandom");

        this.salt = salt;
        this.secureRandom = secureRandom;
    }

    @Override protected byte[] doEncrypt(byte[] plaintextData, byte[] key) {
        // Create random initialization vector
        byte[] initializationVector = new byte[BLOCK_LENGTH_BITS/8];
        secureRandom.nextBytes(initializationVector);

        // Create cipher to encrypt with
        final BufferedBlockCipher cipher = createCipher(true, key, initializationVector);

        // Do encryption
        byte[] encryptedData = process(plaintextData, cipher, "encrypting");

        // Prefix the initialization vectors
        encryptedData = ByteArrayUtils.concatenateByteArrays(initializationVector, encryptedData);

        return encryptedData;
    }

    @Override protected byte[] doDecrypt(byte[] encryptedData, byte[] key) {
        // Get prepended initialization vector
        byte[] initializationVector = Arrays.copyOf(encryptedData, BLOCK_LENGTH_BYTES);
        encryptedData = ByteArrayUtils.removeArrayPrefix(encryptedData, BLOCK_LENGTH_BYTES);

        // Create cipher to decrypt with
        final BufferedBlockCipher cipher = createCipher(false, key, initializationVector);

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


    private BufferedBlockCipher createCipher(boolean encrypt, byte[] key, byte[] initializationVector) {
        try {
            // Setup cipher parameters with key and initialization vector
            CipherParameters params = new ParametersWithIV(new KeyParameter(key), initializationVector);

            // Setup cipher with padding
            //BlockCipherPadding padding = new PKCS7Padding(); // This keeps failing with "pad block corrupted", using TBCPadding for now TODO Figure out why
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new TBCPadding());
            cipher.reset();

            cipher.init(encrypt, params);

            return cipher;
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem when creating cipher: " + e.getMessage(), e);
        }
    }

    /**
     * @param password password to use for generating the key.
     * @return a key of the correct length for the cipher used, based on the specified password.
     */
    protected byte[] generateKey(char[] password) {
        try {
            // Generate raw AES key from password and salt
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_HASH_ALGORITHM, PROVIDER);
            KeySpec spec = new PBEKeySpec(password, salt, KEY_HASH_ROUNDS, KEY_LENGTH_BITS);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_ALGORITHM);
            return secret.getEncoded();
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem when generating key from password: " + e.getMessage(), e);
        }
    }


    @Override
    public int getKeyLengthBits() {
        return KEY_LENGTH_BITS;
    }

    @Override
    public int getBlockLengthBits() {
        return BLOCK_LENGTH_BITS;
    }
}
