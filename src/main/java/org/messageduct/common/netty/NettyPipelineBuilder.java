package org.messageduct.common.netty;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.flowutils.serializer.*;
import org.messageduct.serverinfo.ServerInfo;
import org.messageduct.common.NetworkConfig;

/**
 * Builds the parts of the channel pipeline that are common to the client and server, to avoid duplicating code.
 */
public final class NettyPipelineBuilder {

    /**
     * Creates and sets up common filters for the networking pipeline on the server side.
     *
     * @param networkConfig configuration options for the filters.
     * @param pipeline channel pipeline to add filters to.
     */
    public static void buildCommonServerHandlers(final NetworkConfig networkConfig, ChannelPipeline pipeline) {
        buildCommonHandlers(networkConfig, pipeline, null, false);
    }

    /**
     * Creates and sets up common filters for the networking pipeline on the client side.
     *
     * @param networkConfig configuration options for the filters.
     * @param pipeline channel pipeline to add filters to.
     * @param serverInfo information about the server to connect to.
     */
    public static void buildCommonClientHandlers(final NetworkConfig networkConfig,
                                                 ChannelPipeline pipeline,
                                                 ServerInfo serverInfo) {
        buildCommonHandlers(networkConfig, pipeline, serverInfo, true);
    }

    /**
     * Creates and sets up common filters for the networking pipeline.
     *
     * @param networkConfig configuration options for the filters.
     * @param pipeline channel pipeline to add filters to.
     * @param serverInfo information about the server to connect to.  Needed on the client side, not needed on server side.
     * @param client true if the filters are built for the client, false if they are built for the server.
     */
    public static void buildCommonHandlers(final NetworkConfig networkConfig,
                                           ChannelPipeline pipeline,
                                           ServerInfo serverInfo,
                                           boolean client) {

        // Send messages with length field
        int bytesNeededToDescribeMessageSize = bytesNeededToRepresentNumber(networkConfig.getMaximumMessageSize());
        pipeline.addLast(new LengthFieldPrepender(
                bytesNeededToDescribeMessageSize,
                false));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                networkConfig.getMaximumMessageSize(),
                0,
                bytesNeededToDescribeMessageSize,
                0,
                bytesNeededToDescribeMessageSize));

        // Encrypt/decrypt traffic on the connection if encryption is enabled
        if (networkConfig.isEncryptionEnabled()) {
            if (client) {
                // Client side, pass in server public key
                pipeline.addLast(new EncryptionCodec(serverInfo.getPublicKey()));
            }
            else {
                // Server side, pass in server public and private keys
                pipeline.addLast(new EncryptionCodec(networkConfig.getServerKeys()));
            }
        }

        // Compress/decompress traffic if compression is enabled
        if (networkConfig.isCompressionEnabled()) {
            pipeline.addLast(new JdkZlibEncoder());
            pipeline.addLast(new JdkZlibDecoder());
        }

        // Encode/Decode traffic between Java Objects and binary data
        pipeline.addLast(new MessageSerializerCodec(networkConfig.getMaximumMessageSize(), networkConfig.getAllowedClasses()));

        // Log messages if desired
        if (networkConfig.isMessageLoggingEnabled()) {
            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        }

    }

    private static int bytesNeededToRepresentNumber(final int number) {
        return (int) Math.ceil(Math.floor(log2(number) + 1) / 8);
    }

    private static double log2(final double value) {
        return Math.log(value) / Math.log(2);
    }

    private NettyPipelineBuilder() {
    }
}
