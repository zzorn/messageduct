package org.messageduct.common.mina;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.messageduct.common.NetworkConfig;
import org.messageduct.common.mina.DelegatingHandler;

import javax.crypto.SecretKey;

import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.PublicKey;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;
import static org.messageduct.common.mina.EncryptionHandler.State.*;

/**
 * Handles handshaking and encryption, and delegates decrypted messages to a specified delegate.
 *<ul>
 * <li>The client creates a temporary public and private key using RSA with a large keysize, and sends the public key to the server. </li>
 * <li>The server creates a long random AES key, then encrypts it with the public key provided by the client,
 * and sends the encrypted AES key to the client. </li>
 * <li>The client decrypts the AES key. </li>
 * <li>Subsequent messages between the client and server are encrypted with the AES key, until the end of the session. </li>
 * <li>An unique RSA and AES key are created for each session. </li>
 * </ul>
 * @deprecated can't handle writeMessage.
 */
public class EncryptionHandler extends DelegatingHandler {

    private static final Charset UTF8             = Charset.forName("UTF8");
    private static final String  CLIENT_HANDSHAKE = "DuctEncV01";

    private static final String STATE_KEY      = "MESSAGE_DUCT_ENCRYPTION_STATE";
    private static final String KEYS_KEY       = "MESSAGE_DUCT_ENCRYPTION_KEYS";
    private static final String SECRET_KEY     = "MESSAGE_DUCT_ENCRYPTION_PASSWORD";
    private static final String CLIENT_PUB_KEY = "MESSAGE_DUCT_ENCRYPTION_CLIENT_PUBKEY";

    private final NetworkConfig networkConfig;
    private final boolean clientSide;

    /**
     * @param networkConfig configuration for network settings, in particular the encryption settings are used by this handler.
     * @param handlerDelegate handler to delegate decrypted messages to.
     * @param clientSide if true this handler is on the client side, if false it is on the server side.
     */
    public EncryptionHandler(NetworkConfig networkConfig, IoHandler handlerDelegate, boolean clientSide) {
        super(handlerDelegate);

        notNull(networkConfig, "networkConfig");

        this.clientSide = clientSide;
        this.networkConfig = networkConfig;
    }

    @Override public void sessionCreated(IoSession session) throws Exception {
        setState(session, clientSide ? CLIENT_INITIALIZED : SERVER_INITIALIZED);

        super.sessionCreated(session);
    }

    @Override public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override public void messageReceived(IoSession session, Object message) throws Exception {
        if (!networkConfig.isEncryptionEnabled()) {
            super.messageReceived(session, message);
        }
        else {

            // If we have not yet completed the handshake, complete it

            // Decrypt message

            super.messageReceived(session, message);

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
