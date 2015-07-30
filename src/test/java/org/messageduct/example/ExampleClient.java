package org.messageduct.example;

import com.esotericsoftware.minlog.Log;
import org.flowutils.serializer.KryoSerializer;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.ServerListenerAdapter;
import org.messageduct.client.netty.NettyClientNetworking;
import org.messageduct.common.DefaultNetworkConfig;
import org.messageduct.utils.encryption.AsymmetricEncryption;
import org.messageduct.utils.encryption.RsaEncryption;

import java.awt.*;
import java.security.KeyPair;

/**
 *
 */
public class ExampleClient {

    public static DefaultNetworkConfig createNetworkConfig() {
        final DefaultNetworkConfig networkConfig = new DefaultNetworkConfig();
        networkConfig.registerAllowedClasses(Color.class,
                                             SayMessage.class,
                                             HearMessage.class);
        networkConfig.setCompressionEnabled(true);
        networkConfig.setEncryptionEnabled(false);
        networkConfig.setMessageLoggingEnabled(true);

        // Turn on Kryo logging
        Log.TRACE = false;
        Log.DEBUG = true;
        Log.INFO = true;
        Log.WARN = true;
        Log.ERROR = true;

        return networkConfig;
    }


    public static void main(String[] args) {

        final DefaultNetworkConfig networkConfig = createNetworkConfig();

        ClientNetworking clientNetworking = new NettyClientNetworking();

        clientNetworking.addListener(new ServerListenerAdapter() {
            @Override public void onMessage(ClientNetworking clientNetworking, Object message) {
                System.out.println("ExampleClient.onMessage");
                System.out.println("message = " + message);
            }

            @Override public void onConnected(ClientNetworking clientNetworking) {
                System.out.println("ExampleClient.onConnected");
            }

            @Override
            public void onLoggedIn(ClientNetworking clientNetworking, LoginSuccessMessage loginSuccessMessage) {
                System.out.println("ExampleClient.onLoggedIn");
            }

            @Override
            public void onAccountCreated(ClientNetworking clientNetworking,
                                         CreateAccountSuccessMessage createAccountSuccessMessage) {
                System.out.println("ExampleClient.onAccountCreated");
            }
        });

        clientNetworking.connect(networkConfig, "localhost", networkConfig.getPort());
        clientNetworking.createAccount("foo2", "superLongSecritPass".toCharArray());
        clientNetworking.sendMessage(new SayMessage("Hi server, client here"));
    }

}
