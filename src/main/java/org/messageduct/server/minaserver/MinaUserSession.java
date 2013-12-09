package org.messageduct.server.minaserver;

import org.apache.mina.core.session.IoSession;
import org.flowutils.Check;
import org.messageduct.server.UserSession;

/**
 * Wrapper for a mina IoSession that implements UserSession.
 */
public final class MinaUserSession implements UserSession {

    private final IoSession ioSession;
    private final String userName;

    public MinaUserSession(IoSession ioSession, String userName) {
        Check.notNull(ioSession, "ioSession");
        Check.nonEmptyString(userName, "userName");

        this.ioSession = ioSession;
        this.userName = userName;
    }

    @Override public String getUserName() {
        return userName;
    }

    @Override public void sendMessage(Object message) {
        ioSession.write(message);
    }

    @Override public void disconnectUser() {
        ioSession.close(false);
    }

    @Override public String toString() {
        return "MinaUserSession{" +
               "ioSession=" + ioSession +
               ", userName='" + userName + '\'' +
               '}';
    }
}
