package org.messageduct;

import org.junit.Assert;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.account.DefaultAccountService;
import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.persistence.MemoryAccountPersistence;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.ServerListener;
import org.messageduct.client.ServerListenerAdapter;
import org.messageduct.client.ServerSession;
import org.messageduct.client.mina.MinaClientNetworking;
import org.messageduct.client.serverinfo.DefaultServerInfo;
import org.messageduct.common.DefaultNetworkConfig;
import org.messageduct.server.MessageListener;
import org.messageduct.server.MessageListenerAdapter;
import org.messageduct.server.ServerNetworking;
import org.messageduct.server.UserSession;
import org.messageduct.server.mina.MinaServerNetworking;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test client and server networking communication.
 */
public class NetworkingTest {

    private static final String HELLO_WORLD = "Hello World!";

    @Test
    public void testMessageSending() throws Exception {

        final String username = "foo";
        final char[] password = "longsecritpass".toCharArray();

        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean serverStarted = new AtomicBoolean(false);
        final AtomicBoolean clientLoggedIn = new AtomicBoolean(false);
        final AtomicReference<String> error = new AtomicReference<String>(null);


        final DefaultNetworkConfig networkConfig = new DefaultNetworkConfig();
        networkConfig.registerAllowedClasses(Color.class,
                                             SayMessage.class,
                                             HearMessage.class);
        networkConfig.setCompressionEnabled(false);
        networkConfig.setEncryptionEnabled(true);
        networkConfig.setMessageLoggingEnabled(true);

        final DefaultAccountService accountService = new DefaultAccountService(new MemoryAccountPersistence());
        final ServerNetworking serverNetworking = new MinaServerNetworking(networkConfig, accountService, new MessageListenerAdapter() {
            @Override public void messageReceived(UserSession session, Object message) {
                System.out.println("server.messageReceived");
                System.out.println("message = " + message);

                // First message should be a hello world sent below
                if (!(message instanceof SayMessage)) {
                    error.set("server.messageReceived: Expected a say message, but got: " + message);
                }

                // Send a response
                session.sendMessage(new HearMessage(session.getUserName(), ((SayMessage)message).getText(), Color.GREEN));
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

            @Override public void userIdle(UserSession session) {
                System.out.println("server.userIdle");
                error.set("Server should not have gotten idle");
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


        final ClientNetworking clientNetworking = new MinaClientNetworking(networkConfig);
        Thread clientThread = new Thread(new Runnable() {
            @Override public void run() {


                System.out.println("Calling client init");
                clientNetworking.init();
                System.out.println("Client init called");

                final DefaultServerInfo serverInfo = new DefaultServerInfo("localhost", networkConfig.getPort());

                System.out.println("Calling client login");
                clientNetworking.createAccount(serverInfo, username, password, new ServerListenerAdapter() {
                    @Override public void onConnected(ServerSession serverSession) {
                        System.out.println("client.onConnected");
                        //serverSession.sendMessage(Color.RED);
                    }

                    @Override public void onIdle(ServerSession serverSession) {
                        System.out.println("client.onIdle");
                        error.set("Client should not have gotten idle");
                    }

                    @Override
                    public void onAccountCreated(ServerSession serverSession,
                                                 CreateAccountSuccessMessage createAccountSuccessMessage) {
                        System.out.println("client.onAccountCreated");

                        clientLoggedIn.set(true);

                        assertEquals(username, createAccountSuccessMessage.getUserName());

                        serverSession.sendMessage(new SayMessage(HELLO_WORLD));
                    }

                    @Override public void onMessage(ServerSession serverSession, Object message) {
                        System.out.println("client.onMessage");
                        System.out.println("message = " + message);

                        // We should be getting back a hear message for the message we sent in account created
                        if (!(message instanceof HearMessage)) {
                            error.set("client.onMessage: Expected a hear message, but got: " + message);
                        }

                        serverSession.disconnect();

                    }

                    @Override
                    public void onAccountErrorMessage(ServerSession serverSession,
                                                      AccountErrorMessage accountErrorMessage) {
                        System.out.println("client.onAccountErrorMessage " + accountErrorMessage);

                        error.set("Got error: " + accountErrorMessage);
                    }

                    @Override public void onException(ServerSession serverSession, Throwable e) {
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

                    @Override public void onDisconnected(ServerSession serverSession) {
                        if (!clientLoggedIn.get()) error.set("Client should not have gotten disconnected before managing to create an account");
                    }
                });
                System.out.println("Client login called");

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

        clientNetworking.shutdown();
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
