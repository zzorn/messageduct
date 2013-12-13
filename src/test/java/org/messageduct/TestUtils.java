package org.messageduct;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.fail;

/**
 * Testing related utils.
 */
public class TestUtils {

    /**
     * Runs the runnableTest repeatedly in several threads at the same time, fails if any exception is thrown.
     * @param message message to print with fail
     * @param threadCount number of threads to start simultaneously
     * @param loopCount number of times to run the test in each thread
     * @param runnableTest Runnable implementation with test to run
     */
    public static void testConcurrently(final String message,
                                        final int threadCount,
                                        final int loopCount,
                                        final TestRun runnableTest) {
        final AtomicReference<Throwable> lastException = new AtomicReference<Throwable>(null);

        // Run the serializer test with the specified serializer in many threads at the same time
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    // Loop
                    for (int j = 0; j < loopCount; j++) {
                        // Abort at first error to save time
                        if (lastException.get() != null) return;

                        // Run test code
                        try {
                            runnableTest.run();
                        } catch (Exception e) {
                            throw new Error("Exception in test thread: " + e.getClass() + ": " + e.getMessage(), e);
                        }
                    }
                }
            });
            threads.add(thread);
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread t, Throwable e) {
                    // Store exception
                    lastException.set(e);
                }
            });
            thread.start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Check for error
        final Throwable exception = lastException.get();
        if (exception != null) {
            fail(message + " Last exception thrown: " + exception.getClass() + ": " + exception.getMessage());
            exception.printStackTrace();

            if (exception.getCause() != null) exception.getCause().printStackTrace();

            if (exception.getCause().getCause() != null) exception.getCause().getCause().printStackTrace();
        }
    }


    private TestUtils() {
    }
}
