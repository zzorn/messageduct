package org.messageduct.protocol;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 *
 */
public class BinaryProtocol implements ProtocolCodecFactory {
    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        // TODO
        return null;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        // TODO
        return null;
    }
}
