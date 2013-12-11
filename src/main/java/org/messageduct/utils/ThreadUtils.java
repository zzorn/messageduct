package org.messageduct.utils;

import org.flowutils.Check;

/**
 * Thread related utilities.
 */
public class ThreadUtils {

    /**
     * @return name of the method calling the method that called this method.
     */
    public static String getNameOfCallingMethod() {
        return getNameOfCallingMethod(2);
    }

    /**
     * @param stepsBack how many steps back to look.  1 = method calling the method that called this method.
     * @return name of the method calling the method that called this method if stepsBack is 1, if 2, the method that called that method, and so on.
     *     Null if there was no such method.
     */
    public static String getNameOfCallingMethod(int stepsBack) {
        Check.positiveOrZero(stepsBack, "stepsBack");

        // Go back to method calling this method
        stepsBack += 2;

        // Get trace
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stepsBack >= stackTrace.length) return null;
        else {
            return stackTrace[stepsBack].getMethodName();
        }
    }
}
