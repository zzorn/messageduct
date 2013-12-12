package org.messageduct;

import org.junit.Assert;
import org.junit.Test;
import org.messageduct.utils.UsernameValidator;

import static org.junit.Assert.*;

public class UsernameValidatorTest {

    @Test
    public void testUsernameValidator() throws Exception {
        UsernameValidator usernameValidator = new UsernameValidator();
        usernameValidator.addForbiddenUserName("root");
        usernameValidator.addForbiddenUserName("admin");

        checkUsername(usernameValidator, true, "zop");
        checkUsername(usernameValidator, true, "foobar");
        checkUsername(usernameValidator, true, "FooBar");
        checkUsername(usernameValidator, true, "FooBar5");
        checkUsername(usernameValidator, true, "__leet__");
        checkUsername(usernameValidator, true,  "a2345678901234567890123456789012");
        checkUsername(usernameValidator, false, "a23456789012345678901234567890123");
        checkUsername(usernameValidator, false, "root");
        checkUsername(usernameValidator, false, "RooT");
        checkUsername(usernameValidator, false, "bo");
        checkUsername(usernameValidator, false, "spacy name");
        checkUsername(usernameValidator, false, "spicyNäme");
        checkUsername(usernameValidator, false, "specialName!");
        checkUsername(usernameValidator, false, "1stPosterLol");

    }

    private void checkUsername(UsernameValidator usernameValidator,
                               boolean expected,
                               String name) {
        assertEquals(expected, usernameValidator.check(name) == null);
    }

}
