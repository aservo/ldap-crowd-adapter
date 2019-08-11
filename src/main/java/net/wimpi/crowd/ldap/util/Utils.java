package net.wimpi.crowd.ldap.util;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;


public class Utils {

    public static final String OU_GROUPS = "groups";
    public static final String OU_USERS = "users";

    public static final String CROWD_DN = "dc=crowd";
    public static final String CROWD_GROUPS_DN = "ou=" + Utils.OU_GROUPS + ",dc=crowd";
    public static final String CROWD_USERS_DN = "ou=" + Utils.OU_USERS + ",dc=crowd";

    public static final String MEMBER_OF_AT = "memberOf";

    public static Integer calculateHash(@Nullable String value) {

        if (value == null)
            return 0;

        int hash = value.hashCode();

        if (hash < 0)
            hash *= 31;
        else
            hash *= 13;

        return (Math.abs(hash) % 9999999) + 1;
    }

    public static <T> List<T> nullableSingletonList(T value) {

        if (value == null)
            return Collections.emptyList();

        return Collections.singletonList(value);
    }
}
