package org.messageduct.server;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.messageduct.message.CreateAccountMessage;
import org.messageduct.message.ErrorMessage;
import org.messageduct.message.LoginMessage;

/**
 * Filter that handles login and account creation.
 */
public class AuthenticationFilter extends IoFilterAdapter {

    private static final String ACCOUNT_NAME = "ACCOUNT_NAME";

    private final Authenticator authenticator;

    public AuthenticationFilter(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        String accountName = (String) session.getAttribute(ACCOUNT_NAME);
        if (accountName != null) {
            // Already logged in, forward message
            nextFilter.messageReceived(session, message);
        }
        else {
            // Not logged in, allow only login or create account actions:
            if (message != null && LoginMessage.class.isInstance(message)) {
                // Handle login
                login(session, (LoginMessage) message);
            }
            else if (message != null && CreateAccountMessage.class.isInstance(message)) {
                // Handle account creation
                createAccount(session, (CreateAccountMessage) message);
            }
            else {
                // This message is not allowed here
                sendError(session, new ErrorMessage("NotAuthenticated"));
            }
        }
    }

    protected void login(IoSession session, LoginMessage loginMessage) {
        final ErrorMessage error = authenticator.authenticate(loginMessage.getUsername(), loginMessage.getPassword());
        if (error != null) {
            // Invalid login, close session
            sendError(session, error);
        }
        else {
            // Login ok, store account name in session
            session.setAttribute(ACCOUNT_NAME, loginMessage.getUsername());
        }
    }

    protected void createAccount(IoSession session, CreateAccountMessage createAccountMessage) {
        final ErrorMessage error = authenticator.createAccount(createAccountMessage.getUsername(), createAccountMessage.getPassword());
        if (error != null) {
            // Could not create account, close session
            sendError(session, error);
        }
        else {
            // Account created ok, store account name in session
            session.setAttribute(ACCOUNT_NAME, createAccountMessage.getUsername());
        }
    }

    protected void sendError(IoSession ioSession, ErrorMessage errorMessage) {
        // TODO: Log

        // Send error message to client
        ioSession.write(errorMessage);

        // Close the session on error
        ioSession.close(false);
    }
}
