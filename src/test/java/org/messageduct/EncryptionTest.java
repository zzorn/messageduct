package org.messageduct;

import org.junit.Before;
import org.junit.Test;
import org.messageduct.utils.SecurityUtils;
import org.messageduct.utils.encryption.*;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Random;

import static org.junit.Assert.*;

public class EncryptionTest {

    private static final Charset UTF_8 = Charset.forName("UTF8");
    private static final int RSA_KEY_SIZE = 2048;
    private SymmetricEncryption symmetricEncryption;
    private AsymmetricEncryption asymmetricEncryption;

    @Before
    public void setUp() throws Exception {
        symmetricEncryption = new AesEncryption();
        asymmetricEncryption = new RsaEncryption(RSA_KEY_SIZE);
    }

    @Test
    public void testStringEncryption() throws Exception {
        final String secretMessage = "Secret message: buy more doge";
        final char[] password = "nicedoge".toCharArray();

        final String encryptedString = symmetricEncryption.encrypt(secretMessage, password);
        assertFalse("Should be encrypted", stringEquals(secretMessage, encryptedString));

        final String decryptedString = symmetricEncryption.decrypt(encryptedString, password);
        assertTrue("Should decrypt correctly", stringEquals(secretMessage, decryptedString));

        // Wrong password should throw exception
        try {
            symmetricEncryption.decrypt(encryptedString, "WrongPassword".toCharArray());
            fail("Should throw exception when decrypting with the wrong password");
        }
        catch (WrongPasswordException e) {
            // Ok
        }

        // Test serializing secret key and using it to encrypt something
        final byte[] serialized = symmetricEncryption.serializeSecretKey(symmetricEncryption.generateSecretKeyFromPassword(password));
        final SecretKey secretKey = symmetricEncryption.deserializeSecretKey(serialized);
        final String encryptedString2 = symmetricEncryption.encrypt(secretMessage, secretKey);
        assertFalse("Should be encrypted", stringEquals(secretMessage, encryptedString2));

        final String decryptedString2 = symmetricEncryption.decrypt(encryptedString2, secretKey);
        assertTrue("Should decrypt correctly", stringEquals(secretMessage, decryptedString2));

    }

    @Test
    public void testAsymmetricStringEncryption() throws Exception {
        final String secretMessage = "secret1";

        // Create keypair
        final KeyPair keyPair = asymmetricEncryption.createNewPublicPrivateKey();

        // Encrypt message
        final String encryptedString = asymmetricEncryption.encrypt(secretMessage, keyPair.getPublic());
        assertFalse("Should be encrypted", stringEquals(secretMessage, encryptedString));

        // Decrypt message
        final String decryptedString = asymmetricEncryption.decrypt(encryptedString, keyPair.getPrivate());
        assertTrue("Should decrypt correctly", stringEquals(secretMessage, decryptedString));

        // Wrong password should throw exception
        try {
            asymmetricEncryption.decrypt(encryptedString, asymmetricEncryption.createNewPublicPrivateKey().getPrivate());
            fail("Should throw exception when decrypting with the wrong private key");
        }
        catch (Exception e) {
            // Ok
        }

        // Test serializing public key and using it to encrypt something
        final byte[] serialized = asymmetricEncryption.serializePublicKey(keyPair.getPublic());
        final PublicKey publicKey = asymmetricEncryption.deserializePublicKey(serialized);
        final String encryptedString2 = asymmetricEncryption.encrypt(secretMessage, publicKey);
        assertFalse("Should be encrypted", stringEquals(secretMessage, encryptedString2));

        final String decryptedString2 = asymmetricEncryption.decrypt(encryptedString2, keyPair.getPrivate());
        assertTrue("Should decrypt correctly", stringEquals(secretMessage, decryptedString2));
    }


    @Test
    public void testLotsOfEncryption() throws Exception {
        encryptALot(20, 2000, 1000);
    }

    @Test
    public void testConcurrentEncryption() throws Exception {
        TestUtils.testConcurrently("Encryption should be thread safe", 10, 1, new TestRun() {
            @Override public void run() throws Exception {
                encryptALot(5, 1000, 500);
                encryptALot(5, 1000, 10);
                encryptALot(5, 50, 10);
            }
        });
    }

    private void encryptALot(int loops, final int messageMaxLen, final int passwordMaxLen) throws WrongPasswordException {
        Random random = new Random();
        for (int i = 0; i < loops; i++) {
            encryptSymmetrical(messageMaxLen, passwordMaxLen, random);
        }

        // Asymmetrical is much slower, so run fewer rounds
        for (int i = 0; i < loops / 10; i++) {
            encryptAsymmetrical(random);
        }
    }

    private void encryptSymmetrical(int messageMaxLen, int passwordMaxLen, Random random)
            throws WrongPasswordException {// Generate message and password
        String message = createRandomString(random, messageMaxLen);
        char[] password = createRandomString(random, passwordMaxLen).toCharArray();

        // Check encryption successful
        final String encrypted = symmetricEncryption.encrypt(message, password);
        assertFalse("The message " + message + " should be encrypted", stringEquals(message, encrypted));

        // Check decryption successful
        String decrypted = symmetricEncryption.decrypt(encrypted, password);
        assertTrue("The message " + message + " should decrypt correctly", stringEquals(message, decrypted));
    }

    private void encryptAsymmetrical(Random random) throws WrongPasswordException {
        // Generate public & private key
        final KeyPair keyPair = asymmetricEncryption.createNewPublicPrivateKey();

        // Generate message
        byte[] message = createRandomBytes(random, 64);

        //System.out.println("EncryptionTest.encryptAsymmetrical");
        //System.out.println("  message = '" + message+"'");

        // Check encryption successful
        final byte[] encrypted = asymmetricEncryption.encrypt(message, keyPair.getPublic());
        //System.out.println("  encrypted = '" + encrypted+"'");
        assertFalse("The message " + message + " should be encrypted", byteArrayEquals(message, encrypted));

        // Check decryption successful
        byte[] decrypted = asymmetricEncryption.decrypt(encrypted, keyPair.getPrivate());
        //System.out.println("  decrypted = '" + decrypted+"'");
        assertTrue("The message " + message + " should decrypt correctly", byteArrayEquals(message, decrypted));
    }

    private String createRandomString(Random random, final int maxLen) {
        StringBuilder sb = new StringBuilder();

        final int len = random.nextInt(maxLen);
        for (int j = 0; j < len; j++) {
            sb.append(SecurityUtils.randomAsciiChar());
        }

        return sb.toString();
    }

    private byte[] createRandomBytes(Random random, final int maxLen) {
        final int len = random.nextInt(maxLen);

        byte[] bytes = new byte[len];
        random.nextBytes(bytes);

        return bytes;
    }

    @Test
    public void testArrayEncryption() throws Exception {
        final byte[] secretMessage = "Secret message: buy more doge".getBytes(UTF_8);
        final char[] password = "nicedoge".toCharArray();

        final byte[] encryptedData = symmetricEncryption.encrypt(secretMessage, password);
        assertFalse("Should be encrypted", byteArrayEquals(secretMessage, encryptedData));

        final byte[] decryptedData = symmetricEncryption.decrypt(encryptedData, password);
        assertArrayEquals("Should decrypt correctly", secretMessage, decryptedData);

        // Wrong password should throw exception
        try {
            symmetricEncryption.decrypt(encryptedData, "WrongPassword".toCharArray());
            fail("Should throw exception when decrypting with the wrong password");
        }
        catch (WrongPasswordException e) {
            // Ok
        }
    }

    /* Not currently provided
    @Test
    public void testStreamEncryption() throws Exception {
        final byte[] secretMessage = "Secret message: buy more doge".getBytes(UTF_8);
        final char[] password = "nicedoge".toCharArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final OutputStream encryptionStream = encryptionProvider.createEncryptionStream(outputStream, password);
        encryptionStream.write(secretMessage);
        encryptionStream.flush();
        encryptionStream.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        final InputStream decryptionStream = encryptionProvider.createDecryptionStream(inputStream, password);
        final byte[] decryptedBytes = StreamUtils.readBytesFromInputStream(decryptionStream);

        assertArrayEquals("Should decrypt correctly", secretMessage, decryptedBytes);

        // Wrong password should throw exception
        try {
            ByteArrayInputStream inputStream2 = new ByteArrayInputStream(outputStream.toByteArray());
            encryptionProvider.createDecryptionStream(inputStream2, "WrongPass".toCharArray());

            fail("Should throw exception when decrypting with the wrong password");
        }
        catch (WrongPasswordException e) {
            // Ok
        }

    }
    */


    /**
     * Need to use this, as assertEquals fails for some random unicode strings.
     * @return true if the two strings have identical contents.
     */
    private boolean stringEquals(String a, String b) {
        return byteArrayEquals(a.getBytes(UTF_8), b.getBytes(UTF_8));
    }

    private boolean byteArrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }

        return true;
    }



}
