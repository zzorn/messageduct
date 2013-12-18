package org.messageduct.client;

import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.CreateAccountSuccessMessage;
import org.messageduct.account.messages.LoginSuccessMessage;

/**
 * Adapter for ServerListener.
 * Override the message you are interested in.
 */
public abstract class ServerListenerAdapter implements ServerListener {
    @Override public void onMessage(ServerSession serverSession, Object message) {
    }

    @Override public void onConnected(ServerSession serverSession) {
    }

    @Override public void onDisconnected(ServerSession serverSession) {
    }

    @Override public void onIdle(ServerSession serverSession) {
    }

    @Override public void onLoggedIn(ServerSession serverSession, LoginSuccessMessage loginSuccessMessage) {
    }

    @Override public void onAccountCreated(ServerSession serverSession, CreateAccountSuccessMessage createAccountSuccessMessage) {
    }

    @Override public void onAccountErrorMessage(ServerSession serverSession, AccountErrorMessage accountErrorMessage) {
    }

    @Override public void onException(ServerSession serverSession, Throwable e) {
    }
}
