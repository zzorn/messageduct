package org.messageduct.utils.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.messageduct.utils.ByteArrayUtils;

import java.security.Security;

/**
 *
 */
public class EncryptionUtils {

    /**
     * Installs the bouncy castle encryption provider if not already installed.
     */
    public static void installBouncyCastleProviderIfNotInstalled() {
        if (!containsInstance(BouncyCastleProvider.class, Security.getProviders())) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static <T> boolean containsInstance(final Class<? extends T> item, final T[] items) {
        for (T t : items) {
            if (item.isInstance(t)) return true;
        }
        return false;
    }


    private EncryptionUtils() {
    }

    public static byte[] addPasswordVerificationPrefix(byte[] plaintextData, final byte[] verificationPrefix) {
        if (verificationPrefix != null) {
            plaintextData = ByteArrayUtils.concatenateByteArrays(verificationPrefix, plaintextData);
        }
        return plaintextData;
    }

    public static byte[] verifyPasswordVerificationPrefix(byte[] decryptedData,
                                                    final byte[] verificationPrefix,
                                                    final String decryptionType) throws WrongPasswordException {
        // Check password verification prefix if specified
        if (verificationPrefix != null) {
            // Check length
            if (decryptedData.length < verificationPrefix.length) {
                throw new WrongPasswordException("Wrong " +
                                                 decryptionType +
                                                 " or corrupted data, decrypted data too short for password verification string.");
            }

            // Check prefix
            for (int i = 0; i < verificationPrefix.length; i++) {
                if (decryptedData[i] != verificationPrefix[i]) {
                    throw new WrongPasswordException("Wrong " +
                                                     decryptionType +
                                                     " or corrupted data, password verification string mismatch at character number " +i+"");
                }
            }

            // Remove the prefix from the data
            decryptedData = ByteArrayUtils.removeArrayPrefix(decryptedData, verificationPrefix.length);
        }
        return decryptedData;
    }
}
