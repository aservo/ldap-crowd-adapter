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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.jetbrains.annotations.Nullable;


public class LdapHelper {

    public static Dn createDn(SchemaManager schemaManager, String dnString) {

        try {

            return new Dn(schemaManager, dnString);

        } catch (LdapInvalidDnException e) {

            throw new RuntimeException(e);
        }
    }

    public static Dn createDnWithCn(SchemaManager schemaManager, Dn suffixDn, String entryId) {

        return createDn(schemaManager, String.format("cn=%s,%s", entryId, suffixDn.getName()));
    }

    public static Dn createRootDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "dc=" + id.toLowerCase());
    }

    public static Dn createGroupsDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "ou=" + Utils.OU_GROUPS + ",dc=" + id.toLowerCase());
    }

    public static Dn createUsersDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "ou=" + Utils.OU_USERS + ",dc=" + id.toLowerCase());
    }

    @Nullable
    public static String getGroupFromDn(Dn rootDn, Dn groupDn, Dn queryDn) {

        String attribute = Utils.normalizeAttribute(queryDn.getRdn().getType());

        if ((queryDn.getParent().equals(groupDn) || queryDn.getParent().equals(rootDn)) && (
                attribute.equals(SchemaConstants.CN_AT) ||
                        attribute.equals(SchemaConstants.CN_AT_OID) ||
                        attribute.equals(SchemaConstants.COMMON_NAME_AT))) {

            return queryDn.getRdn().getNormValue();
        }

        return null;
    }

    @Nullable
    public static String getUserFromDn(Dn rootDn, Dn userDn, Dn queryDn) {

        String attribute = Utils.normalizeAttribute(queryDn.getRdn().getType());

        if ((queryDn.getParent().equals(userDn) || queryDn.getParent().equals(rootDn)) && (
                attribute.equals(SchemaConstants.UID_AT) ||
                        attribute.equals(SchemaConstants.UID_AT_OID) ||
                        attribute.equals(SchemaConstants.CN_AT) ||
                        attribute.equals(SchemaConstants.CN_AT_OID) ||
                        attribute.equals(SchemaConstants.COMMON_NAME_AT))) {

            return queryDn.getRdn().getNormValue();
        }

        return null;
    }
}
