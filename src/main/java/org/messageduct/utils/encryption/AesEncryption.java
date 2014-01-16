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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static org.flowutils.Check.notNull;

/**
 * Implementation of EncryptionProvider that uses the Java cryptography API.
 *
 * Uses the bouncy castle implementation, so that we get 256 bit encryption without requiring users to modify their java installation.
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
        super(Arrays.<Class>asList(javax.crypto.spec.SecretKeySpec.class, byte[].class),
              passwordVerificationPrefix);
        notNull(salt, "salt");
        notNull(secureRandom, "secureRandom");

        this.salt = salt;
        this.secureRandom = secureRandom;
    }

    @Override protected byte[] doEncrypt(byte[] plaintextData, SecretKey key) {
        // Create random initialization vector
        byte[] initializationVector = new byte[BLOCK_LENGTH_BITS/8];
        secureRandom.nextBytes(initializationVector);

        // Create cipher to encrypt with
        final BufferedBlockCipher cipher = createCipher(true, key, initializationVector);

        // Do encryption
        byte[] encryptedData = process(plaintextData, cipher, "encrypting");

        // Prefix the initialization vectors
        encryptedData = ByteArrayUtils.concatenate(initializationVector, encryptedData);

        return encryptedData;
    }

    @Override protected byte[] doDecrypt(byte[] encryptedData, SecretKey key) {
        // Get prepended initialization vector
        byte[] initializationVector = ByteArrayUtils.getFirst(encryptedData, BLOCK_LENGTH_BYTES);
        encryptedData = ByteArrayUtils.dropFirst(encryptedData, BLOCK_LENGTH_BYTES);

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


    private BufferedBlockCipher createCipher(boolean encrypt, SecretKey key, byte[] initializationVector) {
        try {
            // Setup AES cipher with CBC mode and TCB padding

            // AES is a state of the art block based cipher which is fast and quite secure when 256 bit keys are used.
            // It is used to encrypt the data in blocks of 128 bits.

            // CBC mode xors the plaintext of each block with the encrypted version of the previous block before encryption
            // (and the first block with the initialization vector), this ensures that each block looks different even
            // if it contains the same data (without a mode like this, the encryption would be very weak).

            // The padding adds some non-constant bits after the end of the data to the last block, to avoid attacks
            // based on knowing the plaintext value at some location.

            // PKCS7Padding might be preferable, but it kept throwing "pad block corrupted" exceptions, so using TBC padding.

            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new TBCPadding());
            cipher.reset();

            // Setup cipher parameters with key and initialization vector
            CipherParameters params = new ParametersWithIV(new KeyParameter(key.getEncoded()), initializationVector);
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
    public SecretKey generateSecretKeyFromPassword(char[] password) {
        try {
            // Generate raw AES key from password and salt
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_HASH_ALGORITHM, PROVIDER);

            KeySpec spec = new PBEKeySpec(password, salt, KEY_HASH_ROUNDS, KEY_LENGTH_BITS);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_ALGORITHM);
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem when generating key from password: " + e.getMessage(), e);
        }
    }


    @Override
    public int getKeyLengthBits() {
        return KEY_LENGTH_BITS;
    }

}
