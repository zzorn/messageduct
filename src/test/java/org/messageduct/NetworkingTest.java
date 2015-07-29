package org.messageduct;

import static org.junit.Assert.*;

import org.flowutils.LogUtils;
import org.junit.Test;
import org.messageduct.account.DefaultAccountService;
import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.persistence.MemoryAccountPersistence;
import org.messageduct.client.ServerListenerAdapter;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.netty.NettyClientNetworking;
import org.messageduct.server.netty.NettyServerNetworking;
import org.messageduct.serverinfo.DefaultServerInfo;
import org.messageduct.common.DefaultNetworkConfig;
import org.messageduct.server.MessageListenerAdapter;
import org.messageduct.server.ServerNetworking;
import org.messageduct.server.UserSession;
import org.messageduct.utils.encryption.AsymmetricEncryption;
import org.messageduct.utils.encryption.RsaEncryption;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test client and server networking communication.
 */
public class NetworkingTest {

    private static final String HELLO_WORLD = "Hello World!";

    @Test
    public void testMessageSending() throws Exception {

        final String serverAddress = "localhost";

        final String username = "foo";
        final char[] password = "longsecritpass".toCharArray();

        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean serverStarted = new AtomicBoolean(false);
        final AtomicBoolean clientLoggedIn = new AtomicBoolean(false);
        final AtomicReference<String> error = new AtomicReference<String>(null);

        AsymmetricEncryption asymmetricEncryption = new RsaEncryption();
        final KeyPair testServerKeys = asymmetricEncryption.createNewPublicPrivateKey();

        final DefaultNetworkConfig networkConfig = new DefaultNetworkConfig();
        networkConfig.registerAllowedClasses(Color.class,
                                             SayMessage.class,
                                             HearMessage.class);
        networkConfig.setCompressionEnabled(false);
        networkConfig.setEncryptionEnabled(false);
        networkConfig.setMessageLoggingEnabled(true);
        networkConfig.setServerKeys(testServerKeys);

        final DefaultAccountService accountService = new DefaultAccountService(new MemoryAccountPersistence());
        final DefaultServerInfo serverInfo = new DefaultServerInfo(serverAddress, networkConfig.getPort());
        final ServerNetworking serverNetworking = new NettyServerNetworking(networkConfig, accountService, serverInfo, new MessageListenerAdapter() {
            @Override public void messageReceived(UserSession session, Object message) {
                System.out.println("server.messageReceived");
                System.out.println("message = " + message);

                // First message should be a hello world sent below
                if (!(message instanceof SayMessage)) {
                    error.set("server.messageReceived: Expected a say message, but got: " + message);
                }

                // Send a response
                session.sendMessage(new HearMessage(session.getUserName(),
                                                    ((SayMessage) message).getText(),
                                                    Color.GREEN));
            }

            @Override public void userDisconnected(UserSession session) {
                System.out.println("server.userDisconnected");

                finished.set(true);
            }

            @Override public void userConnected(UserSession session) {
                System.out.println("server.userConnected");
            }

            @Override public void userCreated(UserSession session) {
                System.out.println("server.userCreated");
            }
        });

        Thread serverThread = new Thread(new Runnable() {
            @Override public void run() {

                System.out.println("Calling server init");
                serverNetworking.init();
                System.out.println("Server init called");

                serverStarted.set(true);
            }
        });


        final ClientNetworking clientNetworking = new NettyClientNetworking();

        Thread clientThread = new Thread(new Runnable() {
            @Override public void run() {

                final DefaultServerInfo serverInfo = new DefaultServerInfo(serverAddress, networkConfig.getPort());

                clientNetworking.addListener(new ServerListenerAdapter() {
                    @Override public void onConnected(ClientNetworking serverSession) {
                        System.out.println("client.onConnected");
                        serverSession.sendMessage(new SayMessage(""));
                        serverSession.sendMessage(Color.RED);
                    }

                    @Override
                    public void onAccountCreated(ClientNetworking serverSession,
                                                 CreateAccountSuccessMessage createAccountSuccessMessage) {
                        System.out.println("client.onAccountCreated");

                        clientLoggedIn.set(true);

                        assertEquals(username, createAccountSuccessMessage.getUserName());

                        serverSession.sendMessage(new SayMessage(HELLO_WORLD));
                    }

                    @Override public void onMessage(ClientNetworking serverSession, Object message) {
                        System.out.println("client.onMessage");
                        System.out.println("message = " + message);

                        // We should be getting back a hear message for the message we sent in account created
                        if (!(message instanceof HearMessage)) {
                            error.set("client.onMessage: Expected a hear message, but got: " + message);
                        }

                        serverSession.disconnect();

                    }

                    @Override
                    public void onAccountErrorMessage(ClientNetworking serverSession,
                                                      AccountErrorMessage accountErrorMessage) {
                        System.out.println("client.onAccountErrorMessage " + accountErrorMessage);

                        error.set("Got error: " + accountErrorMessage);
                    }

                    @Override public void onException(ClientNetworking serverSession, Throwable e) {
                        final String exceptionDesc = "Got exception: " +
                                                     e.getClass().getSimpleName() +
                                                     "" +
                                                     e.getMessage() +
                                                     ": " +
                                                     e;

                        System.out.println("client.onException " + exceptionDesc);

                        e.printStackTrace();
                        error.set(exceptionDesc);
                    }

                    @Override public void onDisconnected(ClientNetworking serverSession) {
                        if (!clientLoggedIn.get())
                            error.set("Client should not have gotten disconnected before managing to create an account");
                    }
                });


                System.out.println("Calling client connect");
                clientNetworking.connect(networkConfig, serverInfo);

                System.out.println("Create account from client");
                clientNetworking.createAccount(username, password);
            }
        });


        serverThread.start();

        // Wait until server started
        while (!serverStarted.get() && error.get() == null) {
            Thread.sleep(100);
        }

        clientThread.start();

        // Wait until done
        while (!finished.get() && error.get() == null) {
            Thread.sleep(100);
        }

        clientNetworking.disconnect();
        serverNetworking.shutdown();

        // Check for errors
        final String errorMessage = error.get();
        if (errorMessage != null) fail(errorMessage);
    }


    public class SayMessage {
        private String text;

        // For serialization
        private SayMessage() {
        }

        public SayMessage(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public class HearMessage {
        private String speaker;
        private String text;
        private Color color;

        // For serialization
        private HearMessage() {
        }

        public HearMessage(String speaker, String text, Color color) {
            this.speaker = speaker;
            this.text = text;
            this.color = color;
        }

        public String getSpeaker() {
            return speaker;
        }

        public String getText() {
            return text;
        }

        public Color getColor() {
            return color;
        }
    }
}
