package org.messageduct.server.mina;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.messageduct.account.AccountService;
import org.messageduct.account.messages.*;

/**
 * Filter that handles login and account creation.
 */
public final class AuthenticationFilter extends IoFilterAdapter {

    private static final String USER_SESSION = "USER_SESSION";

    private final AccountService accountService;

    public AuthenticationFilter(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        System.out.println("AuthenticationFilter.messageReceived");
        System.out.println("  message = " + message);

        if (message == null) {
            // Null message not allowed
            // TODO: Log
            session.close(true);
        }
        else {
            MinaUserSession userSession = (MinaUserSession) session.getAttribute(USER_SESSION);

            AccountResponseMessage response = null;

            if (userSession == null) {
                // Only allow non authenticated messages if not logged in
                if (NonAuthenticatedAccountMessage.class.isInstance(message)) {
                    response = accountService.handleMessage((AccountMessage) message);
                }
                else {
                    // All other messages require that we are logged in
                    response = new AccountErrorMessage("Unauthorized message", "The message " +message.getClass().getSimpleName() + " is not supported when not logged in.  Please log in first.", true);
                }
            }
            else {
                // Handle the message if it is an account related message
                if (AccountMessage.class.isInstance(message)) {
                    AccountMessage accountMessage = (AccountMessage) message;
                    if (!userSession.getUserName().equals(accountMessage.getUsername())) {
                        // Something wrong if username doesn't match session
                        response = new AccountErrorMessage("WrongUsername", "The username in the message ("+accountMessage.getUsername()+") " +
                                                                     "did not match with the logged in username ("+userSession.getUserName()+")", true);
                    } else {
                        // Forward message to account service for handling
                        response = accountService.handleMessage(accountMessage);
                    }
                }
                else {
                    // Forward message to next filter and ultimately the application
                    nextFilter.messageReceived(session, message);
                }
            }

            // Send response to the client if we got any
            if (response != null) {
                session.write(response);

                if (response.shouldCloseConnection()) {
                    // We were asked to close the connection
                    session.close(false); // Let the response get sent first
                }

                // Notify listeners about some types of account message responses
                if (response instanceof LoginSuccessMessage) {
                    // Login ok, store account name in session
                    storeUserSession(session, ((LoginSuccessMessage)response).getUserName());

                    // Notify listeners down the chain about the login
                    nextFilter.messageReceived(session, response);
                }
                else if (response instanceof CreateAccountSuccessMessage) {
                    // Account creation ok, store account name in session
                    storeUserSession(session, ((CreateAccountSuccessMessage)response).getUserName());

                    // Notify listeners down the chain about the account creation
                    nextFilter.messageReceived(session, response);
                }
                else if (response instanceof DeleteAccountSuccessMessage) {
                    // Notify listeners down the chain about the account deletion
                    nextFilter.messageReceived(session, response);
                }
            }
        }
    }

    public static MinaUserSession getUserSession(IoSession ioSession) {
        return (MinaUserSession) ioSession.getAttribute(USER_SESSION);
    }


    private void storeUserSession(IoSession session, String username) {
        session.setAttribute(USER_SESSION, new MinaUserSession(session, username));
    }

    protected void sendError(IoSession ioSession, AccountErrorMessage accountErrorMessage) {
        // TODO: Log
        System.out.println("Authentication error in session "+ioSession+": " + accountErrorMessage);

        // Send error message to client
        ioSession.write(accountErrorMessage);

        // Close the session on error
        ioSession.close(false);
    }


}
