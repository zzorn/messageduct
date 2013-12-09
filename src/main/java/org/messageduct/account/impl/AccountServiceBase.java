package org.messageduct.account.impl;

import org.apache.mina.util.ConcurrentHashSet;
import org.flowutils.Strings;
import org.messageduct.account.AccountService;
import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;
import org.messageduct.account.messages.ErrorMessage;
import org.messageduct.account.persistence.AccountPersistence;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class AccountServiceBase implements AccountService {

    private final Map<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>> messageHandlers = new ConcurrentHashMap<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>>();
    private final Set<Class> acceptedClasses = new ConcurrentHashSet<Class>();


    protected final <T extends AccountMessage> void registerHandler(Class<T> messageType, AccountMessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
    }

    protected final void registerAcceptedClass(Class acceptedClass) {
        acceptedClasses.add(acceptedClass);
    }

    @Override public Set<Class<? extends AccountMessage>> getHandledMessageTypes() {
        return messageHandlers.keySet();
    }

    @Override public Set<Class> getOtherAcceptedClasses() {
        return acceptedClasses;
    }

    @Override public AccountResponseMessage handleMessage(AccountMessage accountMessage) {
        final AccountMessageHandler<AccountMessage> messageHandler = (AccountMessageHandler<AccountMessage>) messageHandlers.get(accountMessage);

        if (messageHandler != null) {
            return messageHandler.handleMessage(accountMessage);
        }
        else {
            return new ErrorMessage("UnknownMessage", "The account message "+accountMessage.getClass() + " is not supported", true);
        }

    }

}
