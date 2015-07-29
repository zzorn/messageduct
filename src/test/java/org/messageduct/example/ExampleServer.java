package org.messageduct.example;

import org.messageduct.account.DefaultAccountService;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;
import org.messageduct.account.persistence.MemoryAccountPersistence;
import org.messageduct.client.ClientNetworking;
import org.messageduct.client.ServerListenerAdapter;
import org.messageduct.common.DefaultNetworkConfig;
import org.messageduct.server.MessageListenerAdapter;
import org.messageduct.server.ServerNetworking;
import org.messageduct.server.UserSession;
import org.messageduct.server.netty.NettyServerNetworking;
import org.messageduct.serverinfo.DefaultServerInfo;
import org.messageduct.utils.encryption.AsymmetricEncryption;
import org.messageduct.utils.encryption.RsaEncryption;

import java.security.KeyPair;

/**
 *
 */
public class ExampleServer {

    public static void main(String[] args) {

        final DefaultNetworkConfig networkConfig = ExampleClient.createNetworkConfig();
        AsymmetricEncryption asymmetricEncryption = new RsaEncryption();
        final KeyPair testServerKeys = asymmetricEncryption.createNewPublicPrivateKey();
        networkConfig.setServerKeys(testServerKeys);

        ServerNetworking serverNetworking = new NettyServerNetworking(networkConfig,
                                                                      new DefaultAccountService(new MemoryAccountPersistence()),
                                                                      new DefaultServerInfo("localhost", networkConfig.getPort()));
        serverNetworking.addMessageListener(new MessageListenerAdapter() {
            @Override public void messageReceived(UserSession session, Object message) {
                System.out.println("ExampleServer.messageReceived");
                System.out.println("message = " + message);
            }

            @Override public void userCreated(UserSession session) {
                System.out.println("ExampleServer.userCreated");
            }

            @Override public void userConnected(UserSession session) {
                System.out.println("ExampleServer.userConnected");
            }

            @Override public void userDisconnected(UserSession session) {
                System.out.println("ExampleServer.userDisconnected");
            }
        });


        serverNetworking.init();

        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

}
