package org.messageduct.utils;

import org.flowutils.LogUtils;

/**
 * TODO: Move to flowutils library
 */
public final class LoggingUtils {

    public static void logAndThrowError(Exception e, final String message) throws IllegalStateException {
        final String fullMessage = message + ": " + e.getMessage();
        LogUtils.getLogger().error(fullMessage, e);
        throw new IllegalStateException(fullMessage, e);
    }

    private LoggingUtils() {
    }
}
