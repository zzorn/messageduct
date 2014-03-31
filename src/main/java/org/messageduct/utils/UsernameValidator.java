package org.messageduct.utils;

import org.apache.mina.util.ConcurrentHashSet;
import org.bouncycastle.util.Strings;
import org.flowutils.Check;
import org.flowutils.StringUtils;

import java.util.*;

/**
 * Validates usernames.
 *
 * Threadsafe.
 */
public class UsernameValidator implements StringValidator {

    public static final int DEFAULT_MIN_LENGTH = 3;
    public static final int DEFAULT_MAX_LENGTH = 32;

    private final Set<String> forbiddenUserNames = new ConcurrentHashSet<String>();
    private final int minUsernameLength;
    private final int maxUsernameLength;

    /**
     * Creates a new username validator.
     */
    public UsernameValidator() {
        this(DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }

    /**
     * Creates a new username validator.
     *
     * @param minUsernameLength minimum allowed username length.
     * @param maxUsernameLength maximum allowed username length.
     * @param forbiddenUserNames zero or more usernames that should not be allowed (case insensitive).
     */
    public UsernameValidator(int minUsernameLength, int maxUsernameLength, String ... forbiddenUserNames) {
        this(minUsernameLength, maxUsernameLength, Arrays.asList(forbiddenUserNames));
    }

    /**
     * Creates a new username validator.
     *
     * @param minUsernameLength minimum allowed username length.
     * @param maxUsernameLength maximum allowed username length.
     * @param forbiddenUserNames usernames that should not be allowed (case insensitive).
     */
    public UsernameValidator(int minUsernameLength, int maxUsernameLength, Collection<String> forbiddenUserNames) {
        Check.positive(minUsernameLength, "minUsernameLength");
        Check.positive(maxUsernameLength, "maxUsernameLength");
        Check.less(minUsernameLength, "minUsernameLength", maxUsernameLength, "maxUsernameLength");

        this.minUsernameLength = minUsernameLength;
        this.maxUsernameLength = maxUsernameLength;

        if (forbiddenUserNames != null) {
            for (String forbiddenUserName : forbiddenUserNames) {
                addForbiddenUserName(forbiddenUserName);
            }
        }
    }

    /**
     * @param forbiddenName a user name that should not be allowed (case insensitive).
     */
    public final void addForbiddenUserName(String forbiddenName) {
        Check.notNull(forbiddenName, "forbiddenName");

        forbiddenUserNames.add(forbiddenName.toLowerCase());
    }

    /**
     * @return true if the specified name is listed among the forbidden usernames (case insensitive).
     */
    public final boolean isForbiddenUserName(String name) {
        return forbiddenUserNames.contains(name.toLowerCase());
    }

    /**
     * @return the currently forbidden usernames.  They are all in lower case, but should be matched case insensitively.
     */
    public final Set<String> getForbiddenUserNames() {
        return Collections.unmodifiableSet(forbiddenUserNames);
    }

    @Override public String check(String userName) {
        if (userName.length() < minUsernameLength) return "The username must be longer than "+minUsernameLength+" characters";
        if (userName.length() > maxUsernameLength) return "The username must be shorter than "+maxUsernameLength+" characters";
        if (!StringUtils.isStrictIdentifier(userName)) return "The username must start with a letter or underscore and contain only letters, underscores or numbers.";
        if (isForbiddenUserName(userName)) return "The username '"+userName+"' is not allowed";

        // Username was ok
        return null;
    }
}
