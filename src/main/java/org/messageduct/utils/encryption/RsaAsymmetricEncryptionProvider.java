package org.messageduct.utils.encryption;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.flowutils.Check;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

/**
 * AsymmetricEncryptionProvider implementation using RSA.
 */
public final class RsaAsymmetricEncryptionProvider extends AsymmetricEncryptionProviderBase {

    static {
        EncryptionUtils.installBouncyCastleProviderIfNotInstalled();
    }

    public static final int MINIMUM_KEY_SIZE = 1024;
    public static final int DEFAULT_KEY_SIZE = 4096;

    private static final String PROVIDER = "BC"; // Use Bouncy Castle provider
    private static final String CIPHER = "RSA/NONE/OAEPWithSHA1AndMGF1Padding";
    private static final String KEYGEN_ALGORITHM = "RSA";

    private final int keySize;

    /**
     * Creates a RsaAsymmetricEncryptionProvider with a default key size, focused on security over performance.
     */
    public RsaAsymmetricEncryptionProvider() {
        this(DEFAULT_KEY_SIZE);
    }

    /**
     * @param keySize the keysize to use.  2048 is a recommended minimum, 4096 is recommended if performance is not an issue.
     */
    public RsaAsymmetricEncryptionProvider(int keySize) {
        this(keySize, DEFAULT_PASSWORD_VERIFICATION_PREFIX);
    }

    public RsaAsymmetricEncryptionProvider(int keySize, byte[] passwordVerificationPrefix) {
        super(Arrays.<Class>asList(org.bouncycastle.asn1.eac.RSAPublicKey.class,
                                   org.bouncycastle.asn1.pkcs.RSAPublicKey.class,
                                   ASN1ObjectIdentifier.class,
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
        final Cipher cipher = getCipher();

        // Initialize
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid public key: " + e + ": " + e.getMessage(), e);
        }

        // Encrypt
        try {
            return cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            throw new IllegalStateException("Problem when encrypting with "+CIPHER+" cipher: " + e + ": " +e.getMessage(),e);
        }
    }

    @Override protected byte[] doDecrypt(PrivateKey privateKey, byte[] dataToDecrypt) {
        // Get cipher
        final Cipher cipher = getCipher();

        // Initialize
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid private key: " + e + ": " + e.getMessage(), e);
        }

        // Encrypt
        try {
            return cipher.doFinal(dataToDecrypt);
        } catch (Exception e) {
            throw new IllegalStateException("Problem when decrypting with "+CIPHER+" cipher: " + e + ": " +e.getMessage(),e);
        }
    }

    private Cipher getCipher() {
        try {
            return Cipher.getInstance(CIPHER, PROVIDER);
        } catch (Exception e) {
            throw new IllegalStateException("Cipher "+CIPHER +" not available: " +e + ":"+ e.getMessage(), e);
        }
    }
}
