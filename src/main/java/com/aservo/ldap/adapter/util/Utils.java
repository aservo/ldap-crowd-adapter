/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aservo.ldap.adapter.util;

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
