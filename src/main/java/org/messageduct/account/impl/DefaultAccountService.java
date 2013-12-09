package org.messageduct.account.impl;

import org.messageduct.account.AccountService;
import org.messageduct.account.account.Account;
import org.messageduct.account.account.DefaultAccount;
import org.messageduct.account.messages.AccountMessage;
import org.messageduct.account.messages.AccountResponseMessage;
import org.messageduct.account.messages.CreateAccountMessage;
import org.messageduct.account.persistence.AccountPersistence;

import java.util.Set;

/**
 *
 */
public class DefaultAccountService extends AccountServiceBase {

    private final AccountPersistence accountPersistence;

    public DefaultAccountService(AccountPersistence accountPersistence) {
        this.accountPersistence = accountPersistence;

        registerHandler(CreateAccountMessage.class, new AccountMessageHandler<CreateAccountMessage>() {
            @Override public AccountResponseMessage handleMessage(CreateAccountMessage message) {

                final String username = message.getUsername();

                // TODO

                // Check if new users are allowed

                // TODO: Check that the username is valid

                createNewAccountObject(message);

                // TODO: Try to reserve the username (this needs to be syncronized)

                // Check if the username was free

                // TODO: Compose response



                return null;
            }
        });

        // TODO: Register other handlers

    }

    private Account createNewAccountObject(CreateAccountMessage createAccountMessage) {
        // TODO: Hash password
        String passwordHash = "";

        Account account = new DefaultAccount(createAccountMessage.getUsername(),
                                             passwordHash,
                                             createAccountMessage.getEmail(),
                                             createAccountMessage.getPublicKey(),
                                             createAccountMessage.getBitcoinAddress());
        return account;
    }
}
