package org.messageduct.server.mina;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.flowutils.Check;
import org.messageduct.utils.encryption.AesSymmetricEncryptionProvider;
import org.messageduct.utils.encryption.AsymmetricEncryptionProvider;
import org.messageduct.utils.encryption.RsaAsymmetricEncryptionProvider;
import org.messageduct.utils.encryption.SymmetricEncryptionProvider;

import static org.flowutils.Check.*;
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
 *
 */
public class EncryptionFilter extends IoFilterAdapter {

    private final AsymmetricEncryptionProvider asymmetricEncryptionProvider;
    private final SymmetricEncryptionProvider symmetricEncryptionProvider;


    public EncryptionFilter() {
        this(new RsaAsymmetricEncryptionProvider(), new AesSymmetricEncryptionProvider());
    }

    public EncryptionFilter(AsymmetricEncryptionProvider asymmetricEncryptionProvider,
                            SymmetricEncryptionProvider symmetricEncryptionProvider) {
        notNull(asymmetricEncryptionProvider, "asymmetricEncryptionProvider");
        notNull(symmetricEncryptionProvider, "symmetricEncryptionProvider");

        this.asymmetricEncryptionProvider = asymmetricEncryptionProvider;
        this.symmetricEncryptionProvider = symmetricEncryptionProvider;
    }

    // TODO: Implement
}
