package org.messageduct.common.mina;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import static org.flowutils.Check.*;
import static org.flowutils.Check.notNull;

/**
 * An IoHandler implementation that delegates messages to the specified delegate.
 */
// TODO: How to handle messages back to the session?  They'd need to be passed through this added on filter chain type thing as well
public abstract class DelegatingHandler implements IoHandler {

    private final IoHandler delegate;

    protected DelegatingHandler(IoHandler delegate) {
        notNull(delegate, "delegate");

        this.delegate = delegate;
    }

    public final IoHandler getDelegate() {
        return delegate;
    }

    @Override public void sessionCreated(IoSession session) throws Exception {
        delegate.sessionCreated(session);
    }

    @Override public void sessionOpened(IoSession session) throws Exception {
        delegate.sessionOpened(session);
    }

    @Override public void sessionClosed(IoSession session) throws Exception {
        delegate.sessionClosed(session);
    }

    @Override public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        delegate.sessionIdle(session, status);
    }

    @Override public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        delegate.exceptionCaught(session, cause);
    }

    @Override public void messageReceived(IoSession session, Object message) throws Exception {
        delegate.messageReceived(session, message);
    }

    // Note that the MessageDuct library does not use the message object in the messageSent event itself, and
    // does not expose the messageSent event to calling code, so we do not need to revert any changes that
    // we have done to the sent message object (e.g. decrypt an encrypted object).
    @Override public void messageSent(IoSession session, Object message) throws Exception {
        delegate.messageSent(session, message);
    }
}
