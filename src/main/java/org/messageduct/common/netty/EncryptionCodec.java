package org.messageduct.common.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.flowutils.ByteArrayUtils;
import org.flowutils.Check;
import org.flowutils.LogUtils;
import org.messageduct.utils.encryption.*;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

import static org.flowutils.Check.notNull;
import static org.messageduct.common.netty.EncryptionCodec.State.*;

/**
 * Creates an encrypted connection between a client and a server.
 *
 * The session setup goes as follows:
 *
 * The server sends its public key to the client.
 * The client creates a temporary public and private key using RSA with a large keysize.
 * The client also creates half of a random session key password.
 * The client sends its public key and its half of the session pass to the server with a handshake, encrypted with the servers public key.
 *
 * The server waits for the client handshake, decrypts it, creates an own random half of a session pass,
 * derives the session key, and sends the client the servers half of the session pass in a handshake, encrypted with the clients public key.
 *
 * The client waits for the server handshake, decrypts it, and derives the session key from the server and client halves of the session pass.
 *
 * If any messages are sent before the handshake is complete, they are queued and sent when the handshake is completed.
 *
 */
public final class EncryptionCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {

    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final String SERVER_INITIAL_HANDSHAKE_HEADER = "MsgDuctServerInitV01";
    private static final String CLIENT_HANDSHAKE_HEADER = "MsgDuctClientV01";
    private static final String SERVER_HANDSHAKE_HEADER = "MsgDuctServerV01";

    private static final int SESSION_PASS_LENGTH_BYTES = 1024;

    private final boolean clientSide;
    private final AsymmetricEncryption asymmetricEncryption;
    private final SymmetricEncryption  symmetricEncryption;
    private PublicKey serverPublicKey;
    private final PrivateKey serverPrivateKey;
    private final List<byte[]> queuedMessages = new ArrayList<byte[]>();
    private State state;

    private byte[] clientHalfOfSessionPass;
    private SecretKey sessionKey;
    private KeyPair clientKeys;


    /**
     * Default constructor for client side encryption codec.
     * @param serverPublicKey public key of the server to connect to, or null if unknown (if null, man in the middle attacks are possible).
     */
    public EncryptionCodec(PublicKey serverPublicKey) {
        this(new RsaEncryption(), new AesEncryption(), true, serverPublicKey, null);
    }

    /**
     * Default constructor for server side encryption codec.
     * @param serverKeys public and private keys of the server.
     */
    public EncryptionCodec(KeyPair serverKeys) {
        this(new RsaEncryption(), new AesEncryption(), false,
             serverKeys != null ? serverKeys.getPublic() : null,
             serverKeys != null ? serverKeys.getPrivate() : null);
    }

    /**
     * @param asymmetricEncryption public & private key encryption to use in the handshake.
     * @param symmetricEncryption symmetric encryption to encrypt the traffic with.
     * @param clientSide true if this is the client side, false if it is the server side.
     * @param serverPublicKey public key of the server, or null if unknown (if null, man in the middle attacks are possible).
     * @param serverPrivateKey private key of the server.  Should only be provided on the server side.
     */
    public EncryptionCodec(AsymmetricEncryption asymmetricEncryption,
                           SymmetricEncryption symmetricEncryption,
                           boolean clientSide,
                           PublicKey serverPublicKey,
                           PrivateKey serverPrivateKey) {
        notNull(asymmetricEncryption, "asymmetricEncryption");
        notNull(symmetricEncryption, "symmetricEncryption");
        if (!clientSide && serverPrivateKey == null) throw new IllegalArgumentException("Server private key must be provided for server side encryption codec, but it was null");
        if (clientSide && serverPrivateKey != null) throw new IllegalArgumentException("Server private key should not be provided for client side encryption codec");

        this.clientSide = clientSide;
        this.asymmetricEncryption = asymmetricEncryption;
        this.symmetricEncryption = symmetricEncryption;
        this.serverPublicKey = serverPublicKey;
        this.serverPrivateKey = serverPrivateKey;

        state = clientSide ? CLIENT_WAITING_FOR_SERVER_PUBLIC_KEY : SERVER_BEFORE_SENDING_PUBLIC_KEY;
    }

    @Override protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        switch (state) {
            case SERVER_BEFORE_SENDING_PUBLIC_KEY:     // Drop through
            case CLIENT_WAITING_FOR_SERVER_PUBLIC_KEY: // Drop through
            case SERVER_WAITING_FOR_CLIENT_HANDSHAKE:  // Drop through
            case CLIENT_WAITING_FOR_SERVER_HANDSHAKE:
                // Queue message until the handshake is ready
                queueMessage(msg);
                break;
            case CONNECTED:
                // Encrypt message with session key and send it on
                try {
                    out.add(symmetricEncryption.encrypt(byteBufToByteArray(msg), sessionKey));
                } catch (Exception e) {
                    protocolError(ctx, "could not encrypt a message: " + e.getMessage());
                }
                break;
            case PROTOCOL_ERROR:
                // Silently drop message
                break;
            case CLOSED:
                // Silently drop message
                break;
            default:
                protocolError(ctx, "got unexpected state: " + state);
        }
    }

    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        switch (state) {
            case SERVER_BEFORE_SENDING_PUBLIC_KEY:
                // We do not expect anything from the client yet
                protocolError(ctx, "did not expect an incoming message from the client before sending public key");
                break;
            case CLIENT_WAITING_FOR_SERVER_PUBLIC_KEY:
                // Decode server public key and create client handshake
                final byte[] encryptedClientHandshake = handleServerPublicKeyMessageOnClient(ctx, msg);

                // Update state
                setState(CLIENT_WAITING_FOR_SERVER_HANDSHAKE);

                // Send handshake
                ctx.writeAndFlush(encryptedClientHandshake);

                break;
            case SERVER_WAITING_FOR_CLIENT_HANDSHAKE:
                // Receive client handshake and construct server handshake in response
                final byte[] encryptedServerHandshake = handleClientHandshakeOnServer(ctx, msg);

                // Send server handshake to client
                out.add(encryptedServerHandshake);

                // Server is now connected
                setState(CONNECTED);

                // Send any messages that were queued while the handshake was ongoing.
                sendQueuedMessages(out);

                break;
            case CLIENT_WAITING_FOR_SERVER_HANDSHAKE:
                // Receive server handshake and construct the session key from it
                handleServerHandshakeOnClient(ctx, msg);

                setState(CONNECTED);

                // Send any messages that were queued while the handshake was ongoing.
                sendQueuedMessages(out);

                break;
            case CONNECTED:
                System.out.println("EncryptionCodec.decode");
                System.out.println("msg = " + msg);
                // Decrypt the message with the session key
                try {
                    out.add(symmetricEncryption.decrypt(byteBufToByteArray(msg), sessionKey));
                } catch (Exception e) {
                    protocolError(ctx, "could not decrypt a message: " + e.getMessage());
                }
                break;
            case PROTOCOL_ERROR:
                // Ignore any further messages after we had a protocol error
                break;
            case CLOSED:
                // Ignore any further messages after close
                break;
            default:
                protocolError(ctx, "got unexpected state: " + state);
        }

    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
        startHandshakeIfNecessary(ctx);
        super.channelActive(ctx);
    }

    @Override public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        startHandshakeIfNecessary(ctx);
        super.handlerAdded(ctx);
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        onConnectionClosed();
        super.channelInactive(ctx);
    }

    @Override public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        onConnectionClosed();
        super.handlerRemoved(ctx);
    }

    private void startHandshakeIfNecessary(ChannelHandlerContext ctx) {
        if (!isClientSide()) {
            // If public key has not yet been sent, do it
            if (state == SERVER_BEFORE_SENDING_PUBLIC_KEY) {
                // Create public key message
                final byte[] serverPublicKeyMessage = ByteArrayUtils.composeWithSizePrefixes(
                        SERVER_INITIAL_HANDSHAKE_HEADER.getBytes(ASCII),
                        asymmetricEncryption.serializePublicKey(this.serverPublicKey)
                );

                // Update state
                setState(SERVER_WAITING_FOR_CLIENT_HANDSHAKE);

                // Send public key
                ctx.writeAndFlush(serverPublicKeyMessage);
            }
        }
    }

    private byte[] handleServerPublicKeyMessageOnClient(ChannelHandlerContext ctx, ByteBuf msg) throws ProtocolException {
        // Decode public key message
        byte[] handshake = byteBufToByteArray(msg);

        // Decompose into parts and check the header block
        List<byte[]> handshakeParts = decomposeAndCheckHandshake(ctx, handshake, "server", 2, SERVER_INITIAL_HANDSHAKE_HEADER);

        // Read server public key
        PublicKey reportedServerPublicKey = deserializePublicKey(ctx, handshakeParts.get(1), "server");

        // Check the server public key, if we have one
        if (serverPublicKey != null) {
            if (!serverPublicKey.equals(reportedServerPublicKey)) {
                // Public key mismatch, impostor possible, refuse to connect
                protocolError(ctx, "Server public key mismatch, we expected " + serverPublicKey + ", " +
                                   "but the server claimed " + reportedServerPublicKey + ".  " +
                                   "Risk for man-in-the middle attack, terminating connection.");
            }
        }
        else {
            // Store the received server public key if we didn't have one from before
            serverPublicKey = reportedServerPublicKey;
        }

        // Return client handshake
        return createClientHandshakeToServer(serverPublicKey);
    }

    private byte[] createClientHandshakeToServer(PublicKey serverPublicKey) {
        // Create random client half of session pass
        clientHalfOfSessionPass = generateSessionPassHalve();

        // Create client keypair
        clientKeys = asymmetricEncryption.createNewPublicPrivateKey();
        final byte[] clientPublicKey = asymmetricEncryption.serializePublicKey(clientKeys.getPublic());

        // Create handshake by composing the byte buffers above
        final byte[] handshake = ByteArrayUtils.composeWithSizePrefixes(
                CLIENT_HANDSHAKE_HEADER.getBytes(ASCII),
                clientHalfOfSessionPass,
                clientPublicKey
        );

        // Encrypt handshake with the servers public key
        return asymmetricEncryption.encrypt(handshake, serverPublicKey);
    }

    private byte[] handleClientHandshakeOnServer(ChannelHandlerContext ctx, ByteBuf msg) throws ProtocolException {
        // Get handshake as byte array
        byte[] encryptedClientHandshake = byteBufToByteArray(msg);

        // Decrypt
        byte[] clientHandshake = decrypt(ctx, encryptedClientHandshake, serverPrivateKey, "client");

        // Decompose into parts and check the header block
        final List<byte[]> handshakeParts = decomposeAndCheckHandshake(ctx, clientHandshake, "client", 3, CLIENT_HANDSHAKE_HEADER);

        // Read client half of session pass
        byte[] clientHalfOfSessionPass = deserializeSessionPass(ctx, handshakeParts.get(1), "client");

        // Read client public key
        PublicKey clientPublicKey = deserializePublicKey(ctx, handshakeParts.get(2), "client");

        // Return server handshake
        return createServerHandshake(clientHalfOfSessionPass, clientPublicKey);
    }

    private byte[] createServerHandshake(byte[] clientHalfOfSessionPass, PublicKey clientPublicKey) {
        // Create server half of session pass
        byte[] serverHalfOfSessionPass = generateSessionPassHalve();

        // Combine client and server session pass to get session key
        sessionKey = createSessionKey(clientHalfOfSessionPass, serverHalfOfSessionPass);

        // Create server handshake to client
        final byte[] serverHandshake = ByteArrayUtils.composeWithSizePrefixes(
                SERVER_HANDSHAKE_HEADER.getBytes(ASCII),
                serverHalfOfSessionPass
        );

        // Encrypt handshake using client public key
        return asymmetricEncryption.encrypt(serverHandshake, clientPublicKey);
    }

    private void handleServerHandshakeOnClient(ChannelHandlerContext ctx, ByteBuf msg) throws ProtocolException {
        // Get handshake as byte array
        byte[] encryptedServerHandshake = byteBufToByteArray(msg);

        // Decrypt
        byte[] serverHandshake = decrypt(ctx, encryptedServerHandshake, clientKeys.getPrivate(), "server");

        // Decompose into parts
        final List<byte[]> handshakeParts = decomposeAndCheckHandshake(ctx, serverHandshake, "server", 2, SERVER_HANDSHAKE_HEADER);

        // Read server half of session pass
        byte[] serverHalfOfSessionPass = deserializeSessionPass(ctx, handshakeParts.get(1), "server");

        // Combine client and server session pass to get session key
        sessionKey = createSessionKey(clientHalfOfSessionPass, serverHalfOfSessionPass);
    }

    private SecretKey createSessionKey(final byte[] clientHalfOfSessionPass,
                                       final byte[] serverHalfOfSessionPass) {
        notNull(clientHalfOfSessionPass, "clientHalfOfSessionPass");
        notNull(serverHalfOfSessionPass, "serverHalfOfSessionPass");
        Check.equal(clientHalfOfSessionPass.length, "clientHalfOfSessionPass.length", SESSION_PASS_LENGTH_BYTES, "SESSION_PASS_LENGTH_BYTES");
        Check.equal(serverHalfOfSessionPass.length, "serverHalfOfSessionPass.length", SESSION_PASS_LENGTH_BYTES, "SESSION_PASS_LENGTH_BYTES");

        // Xor client and server halves of the session pass and convert them to chars
        char[] combinedSessionPass = new char[SESSION_PASS_LENGTH_BYTES];
        for (int i = 0; i < SESSION_PASS_LENGTH_BYTES; i++) {
            combinedSessionPass[i] = (char) (serverHalfOfSessionPass[i] ^ clientHalfOfSessionPass[i]);
        }

        // Generate the actual key using the combined session pass as a password
        return symmetricEncryption.generateSecretKeyFromPassword(combinedSessionPass);
    }

    private byte[] generateSessionPassHalve() {
        final byte[] sessionPassHalve = new byte[SESSION_PASS_LENGTH_BYTES];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(sessionPassHalve);
        return sessionPassHalve;
    }

    private void onConnectionClosed() {
        setState(CLOSED);
        clearQueuedMessages();
    }

    private void clearQueuedMessages() {
        queuedMessages.clear();
    }

    /**
     * Queues a message until the handshake is complete.
     */
    private void queueMessage(ByteBuf msg) {
        byte[] storedMessage = byteBufToByteArray(msg);
        queuedMessages.add(storedMessage);
    }

    /**
     * Sends any messages that were queued before
     */
    private void sendQueuedMessages(List<Object> out) throws Exception {
        for (byte[] queuedMessage : queuedMessages) {
            final byte[] encryptedMessage = symmetricEncryption.encrypt(queuedMessage, sessionKey);
            out.add(encryptedMessage);
        }

        queuedMessages.clear();
    }

    private List<byte[]> decomposeAndCheckHandshake(ChannelHandlerContext ctx,
                                                    byte[] handshake,
                                                    final String source,
                                                    final int expectedHeaderBlockCount,
                                                    final String expectedHeaderBlock) throws ProtocolException {
        // Decompose into parts
        List<byte[]> handshakeParts = null;
        try {
            handshakeParts = ByteArrayUtils.decomposeWithSizePrefixes(handshake);
        }
        catch (ByteArrayUtils.InvalidBlockSize invalidBlockSize) {
            protocolError(ctx, "got invalid handshake block from " + source + ": " + invalidBlockSize.getMessage());
        }

        // Check number of parts
        if (handshakeParts.size() != expectedHeaderBlockCount) {
            protocolError(ctx, "got invalid number of handshake block parts from "+source+", " +
                               "expected "+ expectedHeaderBlockCount +", " +
                               "got " + handshakeParts.size());
        }

        // Check header
        String headerText = new String(handshakeParts.get(0), ASCII);
        if (!expectedHeaderBlock.equals(headerText)) {
            protocolError(ctx, "got invalid " + source + " handshake header, " +
                               "expected '" + expectedHeaderBlock +"', but got '"+ headerText +"'");
        }

        return handshakeParts;
    }

    private byte[] decrypt(ChannelHandlerContext ctx,
                           byte[] encryptedHandshake,
                           final PrivateKey privateKey,
                           final String source) throws ProtocolException {
        byte[] handshake = null;
        try {
            handshake = asymmetricEncryption.decrypt(encryptedHandshake, privateKey);
        } catch (WrongPasswordException e) {
            protocolError(ctx, "could not decrypt the " + source + " handshake: " + e.getMessage());
        }
        return handshake;
    }

    private PublicKey deserializePublicKey(ChannelHandlerContext ctx,
                                           final byte[] serializedPublicKey,
                                           final String source) throws ProtocolException {
        PublicKey publicKey = null;
        try {
            publicKey = asymmetricEncryption.deserializePublicKey(serializedPublicKey);
            notNull(publicKey, "publicKey");
        }
        catch (Throwable e) {
            protocolError(ctx, "could not deserialize " + source + " public key: " + e.getMessage());
        }

        return publicKey;
    }

    private byte[] deserializeSessionPass(ChannelHandlerContext ctx,
                                          final byte[] serializedSessionPass,
                                          final String source)
            throws ProtocolException {
        if (serializedSessionPass.length != SESSION_PASS_LENGTH_BYTES) {
            protocolError(ctx, "got invalid " + source + " session pass length, expected " +
                               SESSION_PASS_LENGTH_BYTES +" bytes, but got "+ serializedSessionPass.length+" bytes");
        }
        return serializedSessionPass;
    }

    private byte[] byteBufToByteArray(ByteBuf buffer) {
        byte[] byteArray = new byte[buffer.readableBytes()];
        if (byteArray.length > 0) buffer.readBytes(byteArray);
        return byteArray;
    }

    private void protocolError(ChannelHandlerContext ctx, String message) throws ProtocolException {
        final String errorMessage = "We were in state " + state + " " +
                                    "on the " + getSideAsString() + " side " +
                                    "and " + message + ".  " +
                                    "Closing connection.";
        LogUtils.getLogger().warn(errorMessage);
        setState(PROTOCOL_ERROR);
        clearQueuedMessages();
        ctx.close();
        throw new ProtocolException("EncryptionCodec: " + errorMessage);
    }

    private void setState(State state) {
        notNull(state, "state");
        if (!state.isApplicableState(isClientSide())) throw new IllegalArgumentException("The state should be applicable to " + getSideAsString() + ", but it was: " + state);

        this.state = state;
    }

    public State getState() {
        return state;
    }

    private String getSideAsString() {
        return isClientSide() ? "client" : "server";
    }

    public boolean isClientSide() {
        return clientSide;
    }


    public enum State {

        /**
         * Server initial state, the server public key is not yet sent.
         */
        SERVER_BEFORE_SENDING_PUBLIC_KEY(false, true),

        /**
         * Client initial state, waiting for the server public key.
         */
        CLIENT_WAITING_FOR_SERVER_PUBLIC_KEY(true, false),

        /**
         * We are on the server, waiting for the client to send a public key and its half of the session key
         * so we can encrypt and send the server half of the session key to encrypt messages with.
         */
        SERVER_WAITING_FOR_CLIENT_HANDSHAKE(false, true),

        /**
         * We are on the client, waiting for the encrypted server half of the session key from the server to use for encrypting the link.
         */
        CLIENT_WAITING_FOR_SERVER_HANDSHAKE(true, false),

        /**
         * Server or client, after keys have been exchanged and they are ready for exchanging encrypted messages.
         */
        CONNECTED(true, true),

        /**
         * Protocol was not followed, recovery impossible, waiting for session to close.
         */
        PROTOCOL_ERROR(true, true),

        /**
         * Session closed, no further communication possible.
         */
        CLOSED(true, true)
        ;

        private final boolean clientSideState;
        private final boolean serverSideState;

        State(boolean clientSideState, boolean serverSideState) {
            this.clientSideState = clientSideState;
            this.serverSideState = serverSideState;
        }

        /**
         * @return true if the client can have this state.
         */
        public boolean isClientSideState() {
            return clientSideState;
        }

        /**
         * @return true if the server can have this state.
         */
        public boolean isServerSideState() {
            return serverSideState;
        }

        /**
         * @param clientSide true if client side, false if server side.
         * @return true if this state is applicable to the specified side.
         */
        public boolean isApplicableState(boolean clientSide) {
            return clientSide ? isClientSideState() : isServerSideState();
        }
    }
}
