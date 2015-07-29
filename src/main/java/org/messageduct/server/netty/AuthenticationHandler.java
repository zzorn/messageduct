package org.messageduct.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.flowutils.LogUtils;
import org.messageduct.account.AccountService;
import org.messageduct.account.messages.*;

import static org.flowutils.Check.notNull;

/**
 * Pipeline handler that handles authentication of users.
 */
public final class AuthenticationHandler extends ChannelInboundHandlerAdapter {

    private final AccountService accountService;

    private String loggedInAccountName = null;

    /**
     * @param accountService service used to handle login and other account related messages.
     */
    public AuthenticationHandler(AccountService accountService) {
        notNull(accountService, "accountService");

        this.accountService = accountService;
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("AuthenticationHandler.channelActive");
        super.channelActive(ctx);
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {

        System.out.println("AuthenticationHandler.channelRead");
        System.out.println("message = " + message);

        // Null messages not allowed
        if (message == null) {
            protocolError(ctx, "Got null message");
            return;
        }

        // Handle message
        AccountResponseMessage response = null;
        if (loggedInAccountName == null) {
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
                if (!loggedInAccountName.equals(accountMessage.getUsername())) {
                    // Something wrong if username doesn't match session
                    response = new AccountErrorMessage("WrongUsername", "The username in the message ("+accountMessage.getUsername()+") " +
                                                                        "did not match with the logged in username.", true);
                } else {
                    // Forward message to account service for handling
                    response = accountService.handleMessage(accountMessage);
                }
            }
            else {
                // Forward message to next handler and ultimately the application
                super.channelRead(ctx, message);
            }
        }

        // Send response to the client if we got any
        if (response != null) {
            ctx.writeAndFlush(response);

            if (response.shouldCloseConnection()) {
                // We were asked to close the connection
                ctx.close();
            }

            // Notify listeners about some types of account message responses
            if (response instanceof LoginSuccessMessage) {
                // Login ok, store account name
                setLoggedInAccountName(((LoginSuccessMessage) response).getUserName());

                // Notify listeners down the chain about the login
                super.channelRead(ctx, response);
            }
            else if (response instanceof CreateAccountSuccessMessage) {
                // Account creation ok, store account name in session
                setLoggedInAccountName(((CreateAccountSuccessMessage) response).getUserName());

                // Notify listeners down the chain about the account creation
                super.channelRead(ctx, response);
            }
            else if (response instanceof DeleteAccountSuccessMessage) {
                // Notify listeners down the chain about the account deletion
                super.channelRead(ctx, response);
            }
        }
    }

    /**
     * Store accountName of logged in account for this channel.
     */
    private void setLoggedInAccountName(String accountName) {
        this.loggedInAccountName = accountName;
    }

    /**
     * @return account name of logged in user, or null if the user is not logged in.
     */
    public String getLoggedInAccountName() {
        return loggedInAccountName;
    }

    private void protocolError(ChannelHandlerContext ctx, String reason) {
        LogUtils.getLogger().debug("Terminating connection: " + reason);
        ctx.close();
    }
}
