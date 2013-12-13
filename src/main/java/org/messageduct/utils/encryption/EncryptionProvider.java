package org.messageduct.utils.encryption;


/**
 * Easy to use encryption of input / output streams based on a password.
 */
public interface EncryptionProvider {

    /**
     * Encrypt a byte array.
     *
     * Note that this might be somewhat inefficient for frequent use on small arrays,
     * as it re-creates the needed encryption key on each call.
     *
     * @param plaintextData non-encrypted data to encrypt.
     * @param password password to use when encrypting
     * @return new byte array with the encrypted data.
     */
    byte[] encrypt(byte[] plaintextData, char[] password);

    /**
     * Decrypt a byte array
     *
     * Note that this might be somewhat inefficient for frequent use on small arrays,
     * as it re-creates the needed decryption key on each call.
     *
     * @param encryptedData encrypted data to decrypt.
     * @param password password to use when decrypting.
     * @return new byte array with the decrypted data.
     * @throws WrongPasswordException if the password was incorrect, and password detection is enabled.
     */
    byte[] decrypt(byte[] encryptedData, char[] password) throws WrongPasswordException;

    /**
     * Encrypt a string.
     *
     * Note that this might be somewhat inefficient for frequent use on small strings,
     * as it re-creates the needed encryption key on each call.
     *
     * @param plaintextData non-encrypted string to encrypt.
     * @param password password to use when encrypting
     * @return string with the encrypted data in base64 format.
     */
    String encrypt(String plaintextData, char[] password);

    /**
     * Decrypt a string.
     *
     * Note that this might be somewhat inefficient for frequent use on small strings,
     * as it re-creates the needed decryption key on each call.
     *
     * @param encryptedData encrypted text to decrypt, stored in base64 format.
     * @param password password to use when decrypting.
     * @return the decrypted text.
     * @throws WrongPasswordException if the password was incorrect, and password detection is enabled.
     */
    String decrypt(String encryptedData, char[] password) throws WrongPasswordException;


    /* Not currently provided.
    /* *
     * Create an encrypting stream.
     *
     * If password checking is enabled, a password checking prefix is written to the stream.
     *
     * @param target output stream to write encrypted data to encrypt.
     * @param password password to use to encrypt the stream.
     * @return output stream that plaintext data can be written to.
     *         It is the callers responsibility to close the stream once done.
     * /
    OutputStream createEncryptionStream(OutputStream target, char[] password) throws IOException;

    /* *
     * Create a decrypting stream.
     *
     * If password checking is enabled, the password checking prefix is read from the stream, and an exception
     * thrown if the password was wrong.
     *
     * @param source encrypted input stream to decrypt.
     * @param password password to use to decrypt the stream.
     * @return decrypted input stream with the plaintext.
     *         It is the callers responsibility to close the stream once done.
     * @throws WrongPasswordException if the password was incorrect, and password detection is enabled.
     * /
    InputStream createDecryptionStream(InputStream source, char[] password) throws IOException, WrongPasswordException;
    */

}
