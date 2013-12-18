package org.messageduct.server.mina;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.messageduct.utils.encryption.*;
import org.messageduct.utils.encryption.AsymmetricEncryption;
import org.messageduct.utils.encryption.RsaEncryption;

import static org.flowutils.Check.notNull;

/**
 * Creates an encrypted connection between a client and a server.
 *
 * The client creates a temporary public and private key using RSA with a large keysize, and sends the public key to the server.
 * The server creates a long random AES key, then encrypts it with the public key provided by the client,
 * and sends the encrypted AES key to the client.
 * The client decrypts the AES key.
 * Subsequent messages between the client and server are encrypted with the AES key, until the end of the session.
 * An unique RSA and AES key are created for each session.
 *
 * Start with a handshake with supported protocol versions, and negotiate the best one?
 * Or at least start with a protocol version specific handshake.
 *
 */
public class EncryptionFilter extends IoFilterAdapter {

    private final AsymmetricEncryption asymmetricEncryption;
    private final SymmetricEncryption symmetricEncryption;


    public EncryptionFilter() {
        this(new RsaEncryption(), new AesEncryption());
    }

    public EncryptionFilter(AsymmetricEncryption asymmetricEncryption,
                            SymmetricEncryption symmetricEncryption) {
        notNull(asymmetricEncryption, "asymmetricEncryption");
        notNull(symmetricEncryption, "symmetricEncryption");

        this.asymmetricEncryption = asymmetricEncryption;
        this.symmetricEncryption = symmetricEncryption;
    }

    // TODO: Implement
}
