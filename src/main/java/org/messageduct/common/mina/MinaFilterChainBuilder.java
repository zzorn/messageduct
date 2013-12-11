package org.messageduct.common.mina;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.messageduct.common.NetworkConfig;
import org.messageduct.server.mina.SerializerProtocol;

/**
 * Builds the parts of the filter chain that are common to the client and server, to avoid duplicating code.
 */
public final class MinaFilterChainBuilder {

    public static void buildCommonFilters(NetworkConfig networkConfig, DefaultIoFilterChainBuilder filterChain) {
        // Encrypt/decrypt traffic on the connection if encryption is enabled
        if (networkConfig.isEncryptionEnabled()) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            final SslFilter sslFilter;
            try {
                sslFilter = new SslFilter(sslContextFactory.newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Could not create encryption filter for networking: " + e.getMessage(), e);
            }
            sslFilter.setUseClientMode(false);
            sslFilter.setNeedClientAuth(true);
            filterChain.addLast("encryption", sslFilter);
        }

        // Compress/decompress traffic if compression is enabled
        if (networkConfig.isCompressionEnabled()) {
            filterChain.addLast("compress", new CompressionFilter());
        }

        // Encode/Decode traffic between Java Objects and binary data
        filterChain.addLast("codec", new ProtocolCodecFilter(new SerializerProtocol(networkConfig.getSerializer())));
    }


    private MinaFilterChainBuilder() {
    }
}
