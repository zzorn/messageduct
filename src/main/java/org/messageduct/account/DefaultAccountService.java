package org.messageduct.account;

import org.messageduct.account.messages.*;
import org.messageduct.account.model.Account;
import org.messageduct.account.model.DefaultAccount;
import org.messageduct.account.persistence.AccountPersistence;
import org.messageduct.utils.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.flowutils.Check.notNull;

/**
 * A default implementation of the account service.
 *
 * Can be extended if more complex Account objects are desired, or additional account related messages needed.
 */
// TODO: Should the account service contain the account persistence, and handle its lifecycle as well?
public class DefaultAccountService extends AccountServiceBase {

    private final AccountPersistence accountPersistence;
    private final StringValidator userNameValidator;
    private final PasswordValidator passwordValidator;
    private final PasswordHasher passwordHasher;

    private final AtomicBoolean newUsersAllowed = new AtomicBoolean(true);
    private final AtomicBoolean loginAllowed = new AtomicBoolean(true);

    public DefaultAccountService(AccountPersistence accountPersistence) {
        this(accountPersistence, new UsernameValidator());
    }

    public DefaultAccountService(AccountPersistence accountPersistence,
                                 StringValidator userNameValidator) {
        this(accountPersistence, userNameValidator, new PasswordValidatorImpl());
    }

    public DefaultAccountService(final AccountPersistence accountPersistence,
                                 final StringValidator userNameValidator,
                                 final PasswordValidator passwordValidator) {
        this(accountPersistence, userNameValidator, passwordValidator, new BCryptPasswordHasher());
    }

    public DefaultAccountService(final AccountPersistence accountPersistence,
                                 final StringValidator userNameValidator,
                                 final PasswordValidator passwordValidator,
                                 final PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
        notNull(accountPersistence, "accountPersistence");
        notNull(userNameValidator, "userNameValidator");
        notNull(passwordValidator, "passwordValidator");

        this.accountPersistence = accountPersistence;
        this.userNameValidator = userNameValidator;
        this.passwordValidator = passwordValidator;

        registerDefaultHandlers();
    }

    protected void registerDefaultHandlers() {

        registerHandler(CreateAccountMessage.class, createNewAccountHandler());
        registerHandler(LoginMessage.class, createLoginHandler());

        // TODO: Register other handlers
    }

    protected AccountMessageHandler<CreateAccountMessage> createNewAccountHandler() {
        return new AccountMessageHandler<CreateAccountMessage>() {
            @Override public AccountResponseMessage handleMessage(CreateAccountMessage message) {

                // Check if new users are allowed
                if (!isNewUsersAllowed()) return createErrorResponse("NoNewUsersAccepted", "Currently no new users are accepted", true);

                // Check that login is allowed (creating an account automatically logs you in as well)
                if (!isLoginAllowed()) return createErrorResponse("LoginDisabled", "Login is currently disabled.  Check back later.", true);

                // Check that the username is valid
                final String username = message.getUsername();
                final String usernameError = userNameValidator.check(username);
                if (usernameError != null) return createErrorResponse("InvalidUsername", usernameError, false);

                // Check that the username is free
                if (accountPersistence.hasAccount(username)) return createErrorResponse("UsernameTaken", "The username '"+username+"' is already taken by someone else", false);

                // Check that the password is ok
                final String passwordError = passwordValidator.check(message.getPassword(), username);
                if (passwordError != null) return createErrorResponse("InvalidPassword", passwordError, false);

                // Create password hash
                String passwordHash = passwordHasher.hashPassword(message.getPassword());

                // The password is no longer needed in cleartext, so overwrite it with random characters
                message.scrubPassword();

                // Create the account object
                final Account account = createNewAccountObject(message, passwordHash);
                if (account == null) return createErrorResponse("AccountCreationError", "Could not create the account for some reason", true);

                // Try to reserve the username and save the account object (an other thread might have taken the username meanwhile)
                if (!accountPersistence.createAccount(username, account)) return createErrorResponse("AccountStorageError", "There was some problem when saving the account, try again", false);

                // The account was created successfully
                return new CreateAccountSuccessMessage(username);
            }
        };
    }

    protected AccountMessageHandler<LoginMessage> createLoginHandler() {
        return new AccountMessageHandler<LoginMessage>() {
            @Override public AccountResponseMessage handleMessage(LoginMessage message) {

                // Check that login is allowed (creating an account automatically logs you in as well)
                if (!isLoginAllowed()) return createErrorResponse("LoginDisabled", "Login is currently disabled.  Check back later.", true);

                // Get user
                final String username = message.getUsername();
                final Account account1 = accountPersistence.getAccount(username);
                if (account1 == null) return createErrorResponse("UnknownUsername", "No account found for username '"+username+"'", false);

                // Check password
                if (!passwordHasher.isCorrectPassword(message.getPassword(), account1.getPasswordHash())) return createErrorResponse("InvalidPassword", "The password was incorrect", true);
                message.scrubPassword();

                // Login succeeded
                return new LoginSuccessMessage(username);
            }
        };
    }

    /**
     * @return true if new accounts can be created, false if only existing accounts are allowed to log in.
     */
    public final boolean isNewUsersAllowed() {
        return newUsersAllowed.get();
    }

    /**
     * @param newUsersAllowed true if new accounts can be created, false if only existing accounts are allowed to log in.
     */
    public final void setNewUsersAllowed(boolean newUsersAllowed) {
        this.newUsersAllowed.set(newUsersAllowed);
    }

    /**
     * @return true if users are allowed to log in, false if not (e.g. if preparing for maintenance or server overloaded).
     */
    public boolean isLoginAllowed() {
        return loginAllowed.get();
    }

    /**
     * @param loginAllowed true if users are allowed to log in, false if not (e.g. if preparing for maintenance or server overloaded).
     */
    public void setLoginAllowed(boolean loginAllowed) {
        this.loginAllowed.set(loginAllowed);
    }

    /**
     * Creates a new account object.
     *
     * Override if you have a more complicated account object.
     *
     * @param createAccountMessage account creation message.
     * @param passwordHash hash of the password (the password has been scrubbed from the accountCreationMessage.
     * @return new account, or null if account creation failed.
     */
    protected Account createNewAccountObject(CreateAccountMessage createAccountMessage, String passwordHash) {
        return new DefaultAccount(createAccountMessage.getUsername(),
                                  passwordHash,
                                  createAccountMessage.getEmail(),
                                  createAccountMessage.getPublicKey(),
                                  createAccountMessage.getBitcoinAddress());
    }

    protected final AccountPersistence getAccountPersistence() {
        return accountPersistence;
    }

    protected final StringValidator getUserNameValidator() {
        return userNameValidator;
    }

    protected final PasswordValidator getPasswordValidator() {
        return passwordValidator;
    }

    protected final PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    private AccountErrorMessage createErrorResponse(final String type, final String message, final boolean closeConnection) {
        return new AccountErrorMessage(type, message, closeConnection);
    }


}
