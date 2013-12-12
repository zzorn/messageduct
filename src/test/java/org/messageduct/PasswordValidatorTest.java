package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.utils.PasswordValidator;
import org.messageduct.utils.PasswordValidatorImpl;

public class PasswordValidatorTest {

    @Test
    public void testPasswordValidator() throws Exception {
        PasswordValidatorImpl passwordValidator = new PasswordValidatorImpl();
        passwordValidator.addDictionaryWord("juliusceasar");
        passwordValidator.addDictionaryWord("commonsecret");

        checkPassword(passwordValidator, true,  "foobar123456");
        checkPassword(passwordValidator, true,  "SPEciUlCh4r4(€Ԇ<\n\\");
        checkPassword(passwordValidator, false, "longusername");
        checkPassword(passwordValidator, false, "LongUsERnAme");
        checkPassword(passwordValidator, false, "commonsecret");
        checkPassword(passwordValidator, false, "juliusceasar");
        checkPassword(passwordValidator, false, "JuliusCeasar");
        checkPassword(passwordValidator, false, "shortpass12");
        checkPassword(passwordValidator, false, "");
        checkPassword(passwordValidator, false, "aaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        checkPassword(passwordValidator, true,  "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"+
                                                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"+
                                                "12345678901234567890123456789012345678901234567890123456");
        checkPassword(passwordValidator, false, "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"+
                                                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"+
                                                "123456789012345678901234567890123456789012345678901234567");

        PasswordValidator shortPasswordValidator = new PasswordValidatorImpl(8);
        checkPassword(shortPasswordValidator, true,  "foobar12");
        checkPassword(shortPasswordValidator, true,  "foobar123456");
        checkPassword(shortPasswordValidator, false, "foobar1");
        checkPassword(shortPasswordValidator, false, "password");
        checkPassword(shortPasswordValidator, false, "Password");
        checkPassword(shortPasswordValidator, false, "PASSWORD");
        checkPassword(shortPasswordValidator, false, "12345678");
        checkPassword(shortPasswordValidator, false, "xxxxxxxx");
        checkPassword(shortPasswordValidator, false, "oooooooo");

    }

    private void checkPassword(PasswordValidator passwordValidator, boolean valid, final String password) {
        assertEquals(valid, passwordValidator.check(password.toCharArray(), "longusername") == null);
    }
}
