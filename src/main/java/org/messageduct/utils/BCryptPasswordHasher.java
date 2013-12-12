package org.messageduct.utils;

import org.flowutils.Check;

/**
 * Password hasher that uses BCrypt (blowfish style hasher).
 */
public final class BCryptPasswordHasher implements PasswordHasher {

    private static final int DEFAULT_LOG_OF_SALT_ROUNDS = 12;
    private static final int MINIMUM_LOG_OF_SALT_ROUNDS = 8;

    private final int logOfSaltRounds;

    /**
     * Creates new BCryptPasswordHasher with default number of salting rounds.
     */
    public BCryptPasswordHasher() {
        this(DEFAULT_LOG_OF_SALT_ROUNDS);
    }

    /**
     * @param logOfSaltRounds logarithm of the number of salting rounds to do.  So actual rounds are of magnitude 2^logOfSaltRounds.
     */
    public BCryptPasswordHasher(int logOfSaltRounds) {
        Check.greaterOrEqual(logOfSaltRounds, "logOfSaltRounds",
                             MINIMUM_LOG_OF_SALT_ROUNDS, "minimum secure level of salt rounds");

        this.logOfSaltRounds = logOfSaltRounds;
    }

    @Override public String hashPassword(char[] password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(logOfSaltRounds));
    }

    @Override public boolean isCorrectPassword(char[] password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
