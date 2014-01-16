package org.messageduct.utils.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Encryption provider that works with asymmetric encryption (public and private keypairs)
 */
// TODO: Add bytebuffer support
public interface AsymmetricEncryption {

    /**
     * Encrypt the specified data with the provided public key.
     *
     * @param dataToEncrypt unencrypted data to encrypt
     * @param publicKey a public key  of the type used by this provider.
     * @return encrypted data
     */
    byte[] encrypt(byte[] dataToEncrypt, PublicKey publicKey);

    /**
     * Decrypt the specified data with the provided private key.
     *
     * @param dataToDecrypt encrypted data to decrypt
     * @param privateKey a private key  of the type used by this provider.
     * @return decrypted data
     * @throws WrongPasswordException if the private key was incorrect, and wrong password detection is enabled.
     */
    byte[] decrypt(byte[] dataToDecrypt, PrivateKey privateKey) throws WrongPasswordException;

    /**
     * Encrypt the specified string with the provided public key.
     * @param dataToEncrypt unencrypted data to encrypt
     * @param publicKey a public key  of the type used by this provider.
     * @return base 64 encoded encrypted data
     */
    String encrypt(String dataToEncrypt, PublicKey publicKey);

    /**
     * Decrypt the specified string with the provided private key.
     * @param dataToDecrypt base 64 encoded encrypted data to decrypt
     * @param privateKey a private key  of the type used by this provider.
     * @return decrypted data
     * @throws WrongPasswordException if the private key was incorrect, and wrong password detection is enabled.
     */
    String decrypt(String dataToDecrypt, PrivateKey privateKey) throws WrongPasswordException;

    /**
     * Encrypt the specified character array with the provided public key.
     *
     * @param dataToEncrypt unencrypted data to encrypt
     * @param publicKey a public key  of the type used by this provider.
     * @return encrypted data
     */
    byte[] encryptCharacters(char[] dataToEncrypt, PublicKey publicKey);

    /**
     * Decrypt the specified character array with the provided private key.
     *
     * @param dataToDecrypt encrypted data to decrypt
     * @param privateKey a private key  of the type used by this provider.
     * @return decrypted data
     * @throws WrongPasswordException if the private key was incorrect, and wrong password detection is enabled.
     */
    char[] decryptCharacters(byte[] dataToDecrypt, PrivateKey privateKey) throws WrongPasswordException;

    /**
     * Creates a new public and private keypair.
     * @return the public and private key.
     */
    KeyPair createNewPublicPrivateKey();

    /**
     * Serializes the type of public key used by this provider.
     * @param publicKey key to serialize
     * @return public key in serialized form
     */
    byte[] serializePublicKey(PublicKey publicKey);

    /**
     * Serializes the type of public key used by this provider to the specified output stream
     * @param publicKey key to serialize
     * @param outputStream stream to write to
     * @throws IOException thrown if there was problem when writing to the stream
     */
    void serializePublicKey(PublicKey publicKey, OutputStream outputStream) throws IOException;

    /**
     * De-serializes the type of public key this provider uses.
     * @param serializedPublicKey data to deserialize.
     * @return de-serialized PublicKey.
     */
    PublicKey deserializePublicKey(byte[] serializedPublicKey);

    /**
     * De-serializes the type of public key this provider uses from the specified InputStream.
     * @param inputStream source to deserialize from.
     * @return de-serialized PublicKey.
     * @throws IOException if the inputStream did not contain a correctly formatted public key.
     */
    PublicKey deserializePublicKey(InputStream inputStream) throws IOException;


}
