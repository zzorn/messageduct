package org.messageduct.utils.encryption;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.flowutils.Check;
import sun.security.rsa.RSAPadding;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

/**
 * AsymmetricEncryption implementation using RSA.
 */
public final class RsaEncryption extends AsymmetricEncryptionBase {

    static {
        EncryptionUtils.installBouncyCastleProviderIfNotInstalled();
    }

    public static final int MINIMUM_KEY_SIZE = 1024;
    public static final int DEFAULT_KEY_SIZE = 4096;

    private static final String PROVIDER = "BC"; // Use Bouncy Castle provider
    private static final String CIPHER = "RSA/ECB/PKCS1Padding";  // TODO: Get to work with OAEP padding - it fails the tests though.
    private static final String KEYGEN_ALGORITHM = "RSA";

    private final int keySize;

    /**
     * Create a new RsaEncryption with no password verification prefix and a default key size, focused on security over performance.
     */
    public RsaEncryption() {
        this(DEFAULT_KEY_SIZE);
    }

    /**
     * Create a new RsaEncryption with no password verification prefix.
     * @param keySize the keysize to use.  2048 is a recommended minimum, 4096 is recommended if performance is not an issue.
     */
    public RsaEncryption(int keySize) {
        this(keySize, null);
    }

    /**
     * @param keySize the keysize to use.  2048 is a recommended minimum, 4096 is recommended if performance is not an issue.
     * @param passwordVerificationPrefix prefix string for checking whether the decryption key was correct.  Should not be too long for RSA.  Defaults to null.
     */
    public RsaEncryption(int keySize, byte[] passwordVerificationPrefix) {
        super(Arrays.<Class>asList(org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey.class,
                                   BigInteger.class),
              passwordVerificationPrefix);

        Check.greaterOrEqual(keySize, "keySize", MINIMUM_KEY_SIZE, "minimum allowed value");

        this.keySize = keySize;
    }

    @Override public KeyPair createNewPublicPrivateKey() {
        // Get generator
        final KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEYGEN_ALGORITHM, PROVIDER);
        } catch (Exception e) {
            throw new IllegalStateException("Key generation algorithm "+KEYGEN_ALGORITHM+" not available: " +e + ":"+ e.getMessage(), e);
        }

        // Initialize
        keyPairGenerator.initialize(keySize);

        // Generate keys
        return keyPairGenerator.generateKeyPair();
    }



    @Override protected byte[] doEncrypt(PublicKey publicKey, byte[] dataToEncrypt) {
        // Get cipher
        final Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, publicKey);

        // Encrypt
        try {
            return cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            throw new IllegalStateException("Problem when encrypting with "+CIPHER+" cipher: " + e + ": " +e.getMessage(),e);
        }
    }

    @Override protected byte[] doDecrypt(PrivateKey privateKey, byte[] dataToDecrypt) {
        // Get cipher
        final Cipher cipher = getCipher(Cipher.DECRYPT_MODE, privateKey);

        // Encrypt
        try {
            return cipher.doFinal(dataToDecrypt);
        } catch (Exception e) {
            throw new IllegalStateException("Problem when decrypting with "+CIPHER+" cipher: " + e + ": " +e.getMessage(),e);
        }
    }

    private Cipher getCipher(int mode, Key key) {
        // Get cipher
        final Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER, PROVIDER);
        } catch (Exception e) {
            throw new IllegalStateException("Cipher "+CIPHER +" not available: " +e + ":"+ e.getMessage(), e);
        }

        // Initialize
        try {
            cipher.init(mode, key);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key: " + e + ": " + e.getMessage(), e);
        }

        return cipher;
    }
}
