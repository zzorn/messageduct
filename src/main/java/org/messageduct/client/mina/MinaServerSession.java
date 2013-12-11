package org.messageduct.client.mina;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.messageduct.client.serverinfo.ServerInfo;
import org.messageduct.client.ServerSessionBase;
import org.messageduct.common.NetworkConfig;
import org.messageduct.common.mina.MinaFilterChainBuilder;

import static org.flowutils.Check.notNull;

/**
 * Represents a connection to a server.
 */
public class MinaServerSession extends ServerSessionBase {

    private final NetworkConfig networkConfig;
    private NioSocketConnector connector;
    private IoSession session = null;

    public MinaServerSession(ServerInfo serverInfo, NetworkConfig networkConfig) {
        super(serverInfo);

        notNull(networkConfig, "networkConfig");

        this.networkConfig = networkConfig;
    }

    @Override protected void doConnect() {
        if (connector != null) throw new IllegalStateException("Connect already called");
        connector = createConnector();

        // Create handler
        connector.setHandler(new IoHandlerAdapter() {
            @Override public void messageReceived(IoSession session, Object message) throws Exception {
                onMessage(message);
            }

            @Override public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
                onIdle();
            }

            @Override public void sessionClosed(IoSession session) throws Exception {
                onDisconnected();
            }

            @Override public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                onException(cause);
            }

        });

        // Connect
        final ConnectFuture connectFuture = connector.connect(getServerInfo().getAddress());
        connectFuture.addListener(new IoFutureListener<ConnectFuture>() {
            @Override public void operationComplete(ConnectFuture future) {
                try {
                    // Store session
                    session = future.getSession();
                    if (session == null) throw new IllegalStateException("Session not created even though connect future fired");

                    // Notify about connection
                    onConnected();
                }
                catch (Throwable e) {
                    // Some problem, handle
                    onException(e);
                }

            }
        });
    }

    private NioSocketConnector createConnector() {
        NioSocketConnector connector = new NioSocketConnector();

        // Set connect timeout
        connector.setConnectTimeoutMillis(networkConfig.getIdleTimeSeconds() * 1000L);

        // Setup filters (encryption, compression, serialization)
        MinaFilterChainBuilder.buildCommonFilters(networkConfig, connector.getFilterChain());

        return connector;
    }

    @Override protected void doSendMessage(Object message) {
        if (session == null) throw new IllegalStateException("Session not yet opened!");

        // Send message
        session.write(message);
    }

    @Override protected void doDisconnect() {
        // Close session
        if (session != null) {
            final CloseFuture closeFuture = session.close(false);

            // Dispose connector when the session has been closed, to avoid blocking this call.
            closeFuture.addListener(new IoFutureListener<CloseFuture>() {
                @Override public void operationComplete(CloseFuture future) {
                    connector.dispose();
                }
            });
        }
        else if (connector != null) {
            // No session open, dispose should be immediate
            connector.dispose();
        }
    }
}
