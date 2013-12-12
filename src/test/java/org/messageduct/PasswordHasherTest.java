package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.utils.BCryptPasswordHasher;
import org.messageduct.utils.PasswordHasher;

public class PasswordHasherTest {

    @Test
    public void testPasswordHasher() throws Exception {
        checkHasher(new BCryptPasswordHasher());
        checkHasher(new BCryptPasswordHasher(8));
        checkHasher(new BCryptPasswordHasher(10));
    }

    private void checkHasher(final PasswordHasher passwordHasher) {

        checkPassword(passwordHasher, "HUNTER123!");
        checkPassword(passwordHasher, "");
        checkPassword(passwordHasher, " really long password with spaces and stuff.. \"\nfoo");
    }

    private void checkPassword(PasswordHasher passwordHasher, final String password) {
        final String hash = passwordHasher.hashPassword(password.toCharArray());
        assertTrue(passwordHasher.isCorrectPassword(password.toCharArray(), hash));
        assertTrue(!passwordHasher.isCorrectPassword(" ".toCharArray(), hash));
        assertTrue(!passwordHasher.isCorrectPassword("foo".toCharArray(), hash));
    }
}
