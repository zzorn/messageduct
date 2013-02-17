package org.messageduct.server;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 *
 */
public class ConnectionHandler implements IoHandler {

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        // TODO: Implement

    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        // TODO: Implement

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        // TODO: Implement

    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        // TODO: Implement

    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        // TODO: Implement

    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        // TODO: Implement

    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        // TODO: Implement

    }
}
