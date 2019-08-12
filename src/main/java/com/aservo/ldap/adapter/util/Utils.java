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
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
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

    public static String normalizeAttribute(String attribute) {

        if (attribute.equalsIgnoreCase(SchemaConstants.OBJECT_CLASS_AT))
            return SchemaConstants.OBJECT_CLASS_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.UID_NUMBER_AT))
            return SchemaConstants.UID_NUMBER_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.UID_AT))
            return SchemaConstants.UID_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.OU_AT))
            return SchemaConstants.OU_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.CN_AT))
            return SchemaConstants.CN_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.COMMON_NAME_AT))
            return SchemaConstants.COMMON_NAME_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.GN_AT))
            return SchemaConstants.GN_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.GIVENNAME_AT))
            return SchemaConstants.GIVENNAME_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.SN_AT))
            return SchemaConstants.SN_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.SURNAME_AT))
            return SchemaConstants.SURNAME_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.DISPLAY_NAME_AT))
            return SchemaConstants.DISPLAY_NAME_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.MAIL_AT))
            return SchemaConstants.MAIL_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.DESCRIPTION_AT))
            return SchemaConstants.DESCRIPTION_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.MEMBER_AT))
            return SchemaConstants.MEMBER_AT;
        else if (attribute.equalsIgnoreCase(SchemaConstants.UNIQUE_MEMBER_AT))
            return SchemaConstants.UNIQUE_MEMBER_AT;
        else if (attribute.equalsIgnoreCase(Utils.MEMBER_OF_AT))
            return Utils.MEMBER_OF_AT;
        else
            return attribute;
    }
}
