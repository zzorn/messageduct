package org.messageduct;

import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;
import org.messageduct.utils.ThreadUtils;

/**
 *
 */
public class ThreadUtilsTest {
    @Test
    public void testGetCallingMethodName() throws Exception {
        assertEquals("testGetCallingMethodName", ThreadUtils.getNameOfCallingMethod(0));

        foo();
    }

    private void foo() {
        assertEquals("testGetCallingMethodName", ThreadUtils.getNameOfCallingMethod());

        assertEquals("foo", ThreadUtils.getNameOfCallingMethod(0));
        assertEquals("testGetCallingMethodName", ThreadUtils.getNameOfCallingMethod(1));

        bar();
    }

    private void bar() {
        assertEquals("foo", ThreadUtils.getNameOfCallingMethod());

        assertEquals("bar", ThreadUtils.getNameOfCallingMethod(0));
        assertEquals("foo", ThreadUtils.getNameOfCallingMethod(1));
        assertEquals("testGetCallingMethodName", ThreadUtils.getNameOfCallingMethod(2));
    }
}
