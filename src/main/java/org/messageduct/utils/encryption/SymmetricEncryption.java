package org.messageduct.utils.encryption;


import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Easy to use symmetric encryption of input / output streams using a password.
 */
// TODO: Add bytebuffer support
public interface SymmetricEncryption {

    /**
     * Encrypt a byte array.
     *
     * Note that this might be somewhat inefficient for frequent use on small arrays,
     * as it re-creates the needed encryption key on each call.
     *
     * @param plaintextData non-encrypted data to encrypt.
     * @param key key to use when encrypting.  Must be the correct type for the cipher.
     * @return new byte array with the encrypted data.
     */
    byte[] encrypt(byte[] plaintextData, SecretKey key);

    /**
     * Decrypt a byte array.
     *
     * Note that this might be somewhat inefficient for frequent use on small strings,
     * as it re-creates the needed decryption key on each call.
     *
     * @param encryptedData encrypted text to decrypt, stored in base64 format.
     * @param key key to use when decrypting.  Must be of the correct type for this chipher.
     * @return the decrypted text.
     * @throws WrongPasswordException if the password was incorrect, and password detection is enabled.
     */
    byte[] decrypt(byte[] encryptedData, SecretKey key) throws WrongPasswordException;

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

    /**
     * Encrypt a string.
     *
     * Note that this might be somewhat inefficient for frequent use on small strings,
     * as it re-creates the needed encryption key on each call.
     *
     * @param plaintextData non-encrypted string to encrypt.
     * @param key key to use when encrypting.  Must be of the correct type for this chipher.
     * @return string with the encrypted data in base64 format.
     */
    String encrypt(String plaintextData, SecretKey key);

    /**
     * Decrypt a string.
     *
     * Note that this might be somewhat inefficient for frequent use on small strings,
     * as it re-creates the needed decryption key on each call.
     *
     * @param encryptedData encrypted text to decrypt, stored in base64 format.
     * @param key key to use when decrypting.  Must be of the correct type for this chipher.
     * @return the decrypted text.
     * @throws WrongPasswordException if the password was incorrect, and password detection is enabled.
     */
    String decrypt(String encryptedData, SecretKey key) throws WrongPasswordException;

    /**
     * @return a new random key of suitable type for this symmetric encryption.
     */
    SecretKey generateSecretKeyRandomly();

    /**
     * @param password password to use for generating the key.
     * @return a key for the cipher used, based on the specified password.
     */
    SecretKey generateSecretKeyFromPassword(char[] password);

    /**
     * Serialized the secret key, e.g. for sending over an asymmetrically encrypted channel.
     * @param key secret key to serialize.
     * @return the secret key in serialized form
     */
    byte[] serializeSecretKey(SecretKey key);

    /**
     * Deserializes a secret key.
     * @param serializedKey serialized data
     * @return the deserialized secret key.
     */
    SecretKey deserializeSecretKey(byte[] serializedKey);

    /**
     * Serialized a secret key to an output stream.
     * The size of the serialized key is written as well.
     * @param key secret key to serialize.
     * @param outputStream stream to write the key to.
     * @throws IOException thrown if there was some problem when writing to the stream
     */
    void serializeSecretKey(SecretKey key, OutputStream outputStream) throws IOException;

    /**
     * Deserialize a secret key from an input stream.
     * The key is preceded by its size.
     * @param inputStream stream to read from.
     * @return the deserialized secret key.
     * @throws IOException thrown if there was some problem when reading from the stream, or if the key was in an invalid format.
     */
    SecretKey deserializeSecretKey(InputStream inputStream) throws IOException;

    /**
     * @return key length used by this encryption, specified in bits (not bytes).
     */
    int getKeyLengthBits();


}
