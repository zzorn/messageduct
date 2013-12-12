package org.messageduct;

import org.junit.Assert;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.utils.SecurityUtils;

public class SecurityUtilsTest {

    @Test
    public void testScrubChars() throws Exception {
        final String testString = "foobar";

        final char[] chars = testString.toCharArray();
        SecurityUtils.scrubChars(chars);

        final String scrubbedString = String.valueOf(chars);
        assertNotEquals(testString, scrubbedString);
    }

}
