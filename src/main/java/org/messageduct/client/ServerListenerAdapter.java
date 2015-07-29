package org.messageduct.client;

import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;

/**
 * Adapter for ServerListener.
 * Override the message(s) you are interested in.
 */
public abstract class ServerListenerAdapter implements ServerListener {
    @Override public void onMessage(ClientNetworking clientNetworking, Object message) {
    }

    @Override public void onConnected(ClientNetworking clientNetworking) {
    }

    @Override public void onDisconnected(ClientNetworking clientNetworking) {
    }

    @Override public void onLoggedIn(ClientNetworking clientNetworking, LoginSuccessMessage loginSuccessMessage) {
    }

    @Override public void onAccountCreated(ClientNetworking clientNetworking, CreateAccountSuccessMessage createAccountSuccessMessage) {
    }

    @Override public void onAccountErrorMessage(ClientNetworking clientNetworking, AccountErrorMessage accountErrorMessage) {
    }

    @Override public void onException(ClientNetworking clientNetworking, Throwable e) {
    }
}
