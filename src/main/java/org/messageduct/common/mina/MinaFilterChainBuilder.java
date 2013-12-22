package org.messageduct.common.mina;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.messageduct.common.NetworkConfig;
import org.messageduct.server.mina.EncryptionFilter;
import org.messageduct.server.mina.SerializerProtocol;

/**
 * Builds the parts of the filter chain that are common to the client and server, to avoid duplicating code.
 */
public final class MinaFilterChainBuilder {

    /**
     * @param networkConfig configuration options for the filters.
     * @param filterChain filter chain to add filters to.
     * @param client true if the filters are built for the client, false if they are built for the server.
     */
    public static void buildCommonFilters(NetworkConfig networkConfig, DefaultIoFilterChainBuilder filterChain, boolean client) {
        // Encrypt/decrypt traffic on the connection if encryption is enabled
        if (networkConfig.isEncryptionEnabled()) {
            filterChain.addLast("encryption", new EncryptionFilter(client));
        }

        // Compress/decompress traffic if compression is enabled
        if (networkConfig.isCompressionEnabled()) {
            filterChain.addLast("compress", new CompressionFilter());
        }

        // Encode/Decode traffic between Java Objects and binary data
        filterChain.addLast("codec", new ProtocolCodecFilter(new SerializerProtocol(networkConfig.getSerializer())));

        // Log messages if desired
        if (networkConfig.isMessageLoggingEnabled()) {
            filterChain.addLast("logging", new LoggingFilter());
        }
    }


    private MinaFilterChainBuilder() {
    }
}
