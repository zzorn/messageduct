package org.messageduct.server.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.WriteRequestFilter;
import org.messageduct.utils.encryption.AesEncryption;
import org.messageduct.utils.encryption.AsymmetricEncryption;
import org.messageduct.utils.encryption.RsaEncryption;
import org.messageduct.utils.encryption.SymmetricEncryption;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.PublicKey;

import static org.flowutils.Check.notNull;
import static org.messageduct.server.mina.EncryptionFilter.State.*;

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
// TODO: Do the handshake in client/server io handler, using serialized objects, when complete,
    // activate Encryption for that session.
    // Use stream type encryption, subsequent messages can be split or merged,
    // so can not assume that beginning and end are well defined.
public class EncryptionFilter extends WriteRequestFilter {

    private static final Charset UTF8             = Charset.forName("UTF8");
    private static final String  CLIENT_HANDSHAKE = "DuctEncV01";

    private static final String STATE_KEY      = "MESSAGE_DUCT_ENCRYPTION_STATE";
    private static final String KEYS_KEY       = "MESSAGE_DUCT_ENCRYPTION_KEYS";
    private static final String SECRET_KEY     = "MESSAGE_DUCT_ENCRYPTION_PASSWORD";
    private static final String CLIENT_PUB_KEY = "MESSAGE_DUCT_ENCRYPTION_CLIENT_PUBKEY";

    private final AsymmetricEncryption asymmetricEncryption;
    private final SymmetricEncryption  symmetricEncryption;
    private final boolean              clientSide;

    public EncryptionFilter(final boolean clientSide) {
        this(new RsaEncryption(), new AesEncryption(), clientSide);
    }

    public EncryptionFilter(AsymmetricEncryption asymmetricEncryption,
                            SymmetricEncryption symmetricEncryption,
                            boolean clientSide) {
        notNull(asymmetricEncryption, "asymmetricEncryption");
        notNull(symmetricEncryption, "symmetricEncryption");

        this.clientSide = clientSide;
        this.asymmetricEncryption = asymmetricEncryption;
        this.symmetricEncryption = symmetricEncryption;
    }

    @Override
    public void sessionCreated(final NextFilter nextFilter, final IoSession session)
            throws Exception {
        setState(session, clientSide ? CLIENT_INITIALIZED : SERVER_INITIALIZED);

        super.sessionCreated(nextFilter, session);
    }

    @Override
    public void sessionOpened(final NextFilter nextFilter, final IoSession session) throws Exception {
        System.out.println("EncryptionFilter.sessionOpened");
        if (clientSide) {
            // If we are the client, send a first handshake with public key
            final KeyPair keyPair = asymmetricEncryption.createNewPublicPrivateKey();
            System.out.println("EncryptionFilter.sessionOpened - sending handshake");
            //session.write(CLIENT_HANDSHAKE.getBytes(UTF8));

            //TODO: Reply in a way that does not go through the whole chain.
            session.write(asymmetricEncryption.serializePublicKey(keyPair.getPublic()));
            setKeys(session, keyPair);
            setState(session, CLIENT_WAITING_FOR_SESSION_KEY);
        }
        else {
            // Server waits for client handshake
            setState(session, SERVER_WAITING_FOR_CLIENT_PUB_KEY);
        }
    }

    @Override
    public void sessionClosed(final NextFilter nextFilter, final IoSession session) throws Exception {
        setState(session, CLOSED);
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void messageReceived(final NextFilter nextFilter, final IoSession session, final Object message) throws Exception {

        // Get message data
        IoBuffer inBuffer = (IoBuffer) message;
        final int remaining = inBuffer.remaining();

        // Forward empty buffers
        if (remaining <= 0 || !inBuffer.hasRemaining()) {
            nextFilter.messageReceived(session, message);
            return;
        }

        // Read data
        byte[] messageData = new byte[remaining];
        inBuffer.get(messageData);
        inBuffer.flip();

        final State state = getState(session);
        if (!clientSide && state == SERVER_WAITING_FOR_CLIENT_PUB_KEY) {
            // We are waiting for client handshake with public key
            try {
                // Get client public key
                final PublicKey clientPublicKey = asymmetricEncryption.deserializePublicKey(messageData);
                setClientPublicKey(session, clientPublicKey);

                // Generate secret key for session
                final byte[] secretSessionKey = symmetricEncryption.generateNewRandomKey();
                setSecretKey(session, secretSessionKey);

                // Send secret session key to client, encrypted with client public key
                session.write(asymmetricEncryption.encrypt(secretSessionKey, clientPublicKey));

                // Session is ok as soon as client has received it.
                setState(session, CONNECTED);

            } catch (Throwable e) {
                setState(session, PROTOCOL_ERROR);
                session.close(false);
                throw new IOException("Problem establishing an encrypted connection, was expecting client public key, but could not deserialize it: " + e + ": "+ e.getMessage(), e);
            }

            // Forward an open message down the chain
            nextFilter.sessionOpened(session);
        }
        else if (clientSide && state == CLIENT_WAITING_FOR_SESSION_KEY) {
            // We are waiting for encrypted secret key
            try {
                final byte[] secretKey = asymmetricEncryption.decrypt(messageData, getKeys(session).getPrivate());
                setSecretKey(session, secretKey);
                setState(session, CONNECTED);
            } catch (Throwable e) {
                setState(session, PROTOCOL_ERROR);
                session.close(false);
                throw new IOException("Problem establishing an encrypted connection, was expecting secret session key, but could not decrypt it: " + e + ": "+ e.getMessage(), e);
            }

            // Forward an open message down the chain
            nextFilter.sessionOpened(session);
        }
        else if (state == CONNECTED) {
            // Decrypt message using secret key
            final byte[] decryptedData;
            try {
                decryptedData = symmetricEncryption.decrypt(messageData, getSecretKey(session));
            } catch (Throwable e) {
                setState(session, PROTOCOL_ERROR);
                session.close(false);
                throw new IOException("Problem decrypting a message on an encrypted connection: " + e + ": "+ e.getMessage(), e);
            }

            // Forward message to next filter
            nextFilter.messageReceived(session, decryptedData);
        }
        else {
            // Unexpected state, raise error
            setState(session, PROTOCOL_ERROR);
            session.close(false);
            throw new IllegalStateException("Invalid state " + state + " when receiving message.");
        }
    }

    @Override
    protected Object doFilterWrite(final NextFilter nextFilter, final IoSession session, final WriteRequest writeRequest) throws Exception {
        // Get message data
        IoBuffer inBuffer = (IoBuffer) writeRequest.getMessage();
        final int remaining = inBuffer.remaining();

        // Forward empty buffers
        if (remaining <= 0 || !inBuffer.hasRemaining()) {
            return writeRequest.getMessage();
        }

        // Read data
        byte[] messageData = new byte[remaining];
        inBuffer.get(messageData);
        inBuffer.flip();

        final State state = getState(session);
        if (state == CONNECTED) {
            final byte[] encryptedMessage;
            try {
                encryptedMessage = symmetricEncryption.encrypt(messageData, getSecretKey(session));
            } catch (Throwable e) {
                setState(session, PROTOCOL_ERROR);
                session.close(false);
                throw new IOException("Problem encrypting a message on an encrypted connection: " + e + ": "+ e.getMessage(), e);
            }
            return encryptedMessage;
        }
        else {
            throw new IOException("Can not send a message, current state is " + state);
        }
    }

    private State getState(IoSession session) {
        return (State) session.getAttribute(STATE_KEY);
    }

    private void setState(IoSession session, State state) {
        session.setAttribute(STATE_KEY, state);
    }

    private KeyPair getKeys(IoSession session) {
        return (KeyPair) session.getAttribute(KEYS_KEY);
    }

    private void setKeys(IoSession session, KeyPair keys) {
        session.setAttribute(KEYS_KEY, keys);
    }

    private PublicKey getClientPublicKey(IoSession session) {
        return (PublicKey) session.getAttribute(CLIENT_PUB_KEY);
    }

    private void setClientPublicKey(IoSession session, PublicKey key) {
        session.setAttribute(CLIENT_PUB_KEY, key);
    }

    private byte[] getSecretKey(IoSession session) {
        return (byte[]) session.getAttribute(SECRET_KEY);
    }

    private void setSecretKey(IoSession session, byte[] key) {
        session.setAttribute(SECRET_KEY, key);
    }


    public boolean isClientSide() {
        return clientSide;
    }


    public static enum State {
        CLIENT_INITIALIZED,
        SERVER_INITIALIZED,
        CLIENT_WAITING_FOR_SESSION_KEY,
        SERVER_WAITING_FOR_CLIENT_PUB_KEY,

        CONNECTED,

        PROTOCOL_ERROR,
        CLOSED
    }
}
