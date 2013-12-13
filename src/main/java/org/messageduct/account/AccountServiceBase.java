package org.messageduct.account;

import org.messageduct.account.messages.AccountErrorMessage;
import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;
import org.messageduct.utils.service.ServiceBase;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class AccountServiceBase extends ServiceBase implements AccountService {

    private final Map<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>> messageHandlers = new ConcurrentHashMap<Class<? extends AccountMessage>, AccountMessageHandler<? extends AccountMessage>>();


    protected final <T extends AccountMessage> void registerHandler(Class<T> messageType, AccountMessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
    }

    public Set<Class<? extends AccountMessage>> getHandledMessageTypes() {
        return messageHandlers.keySet();
    }

    @Override public AccountResponseMessage handleMessage(AccountMessage accountMessage) {
        final Class<? extends AccountMessage> messageType = accountMessage.getClass();

        ensureActive("Handle " + messageType.getSimpleName());

        final AccountMessageHandler<AccountMessage> messageHandler = (AccountMessageHandler<AccountMessage>) messageHandlers.get(messageType);

        if (messageHandler != null) {
            return messageHandler.handleMessage(accountMessage);
        }
        else {
            return new AccountErrorMessage("UnknownMessage", "The account message of type "+messageType + " is not supported", true);
        }

    }

}
