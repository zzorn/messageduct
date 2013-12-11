package org.messageduct.account;

import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;
import org.messageduct.account.messages.ErrorMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class AccountServiceBase implements AccountService {

    private final Map<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>> messageHandlers = new ConcurrentHashMap<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>>();


    protected final <T extends AccountMessage> void registerHandler(Class<T> messageType, AccountMessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
    }

    public Set<Class<? extends AccountMessage>> getHandledMessageTypes() {
        return messageHandlers.keySet();
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
