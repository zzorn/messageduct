package org.messageduct;

import org.junit.Before;
import org.junit.Test;
import org.messageduct.utils.SecurityUtils;
import org.messageduct.utils.encryption.CipherEncryptionProvider;
import org.messageduct.utils.encryption.EncryptionProvider;
import org.messageduct.utils.encryption.WrongPasswordException;

import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class EncryptionTest {

    private static final Charset UTF_8 = Charset.forName("UTF8");
    private EncryptionProvider encryptionProvider;

    @Before
    public void setUp() throws Exception {
        encryptionProvider = new CipherEncryptionProvider();
    }

    @Test
    public void testStringEncryption() throws Exception {
        final String secretMessage = "Secret message: buy more doge";
        final char[] password = "nicedoge".toCharArray();

        final String encryptedString = encryptionProvider.encrypt(secretMessage, password);
        assertFalse("Should be encrypted", stringEquals(secretMessage, encryptedString));

        final String decryptedString = encryptionProvider.decrypt(encryptedString, password);
        assertTrue("Should decrypt correctly", stringEquals(secretMessage, decryptedString));

        // Wrong password should throw exception
        try {
            encryptionProvider.decrypt(encryptedString, "WrongPassword".toCharArray());
            fail("Should throw exception when decrypting with the wrong password");
        }
        catch (WrongPasswordException e) {
            // Ok
        }
    }


    @Test
    public void testLotsOfEncryption() throws Exception {
        encryptALot(1000, 5000, 1000);
    }

    @Test
    public void testConcurrentEncryption() throws Exception {
        TestUtils.testConcurrently("Encryption should be thread safe", 10, 1, new TestRun() {
            @Override public void run() throws Exception {
                encryptALot(100, 5000, 1000);
                encryptALot(100, 5000, 10);
                encryptALot(100, 50, 10);
            }
        });
    }

    private void encryptALot(int loops, final int messageMaxLen, final int passwordMaxLen) throws WrongPasswordException {
        Random random = new Random();
        for (int i = 0; i < loops; i++) {
            // Generate message and password
            String message = createRandomString(random, messageMaxLen);
            char[] password = createRandomString(random, passwordMaxLen).toCharArray();

            // Check encryption successful
            final String encrypted = encryptionProvider.encrypt(message, password);
            assertFalse("The message " + message + " should be encrypted", stringEquals(message, encrypted));

            // Check decryption successful
            String decrypted = encryptionProvider.decrypt(encrypted, password);
            assertTrue("The message " + message + " should decrypt correctly", stringEquals(message, decrypted));
        }
    }

    private String createRandomString(Random random, final int maxLen) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < random.nextInt(maxLen); j++) {
            sb.append(SecurityUtils.randomChar());
        }

        return sb.toString();
    }

    @Test
    public void testArrayEncryption() throws Exception {
        final byte[] secretMessage = "Secret message: buy more doge".getBytes(UTF_8);
        final char[] password = "nicedoge".toCharArray();

        final byte[] encryptedData = encryptionProvider.encrypt(secretMessage, password);
        assertFalse("Should be encrypted", byteArrayEquals(secretMessage, encryptedData));

        final byte[] decryptedData = encryptionProvider.decrypt(encryptedData, password);
        assertArrayEquals("Should decrypt correctly", secretMessage, decryptedData);

        // Wrong password should throw exception
        try {
            encryptionProvider.decrypt(encryptedData, "WrongPassword".toCharArray());
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
