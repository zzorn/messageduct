package org.messageduct.account.persistence;

import org.messageduct.account.model.Account;
import org.messageduct.utils.service.Service;

/**
 * Interface for storage services required by the AccountService.
 *
 * Should allow multi-threaded access.
 */
public interface AccountPersistence extends Service {

    /**
     * Called when the application is initializing.  Can be used to connect to / load database.
     */
    void init();

    /**
     * Called when the application shuts down.  Close any database connections and the like.
     */
    void shutdown();

    /**
     * @return account for the specified userName, or null if none exists.
     */
    Account getAccount(String userName);

    /**
     * @return true if an account with the specified username exists currently, false if not.
     */
    boolean hasAccount(String userName);

    /**
     * Creates a new account, if one did not already exist for this username.
     * @param userName username of the account.
     * @param account account to create.
     * @return true if the account was created successfully, false if the userName was already taken.
     */
    boolean createAccount(String userName, Account account);

    /**
     * Updates an existing account.
     * @param userName username of the account.
     * @param account new account data.
     * @return true if the account was successfully updated, false if a previous account was not found.
     */
    boolean updateAccount(String userName, Account account);

    /**
     * Deletes an account with the specified username.
     * @return true if the account was successfully deleted, false if an account was not found for that username.
     */
    boolean deleteAccount(String userName);

}
