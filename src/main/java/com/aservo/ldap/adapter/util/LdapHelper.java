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


/**
 * A helper for LDAP handling.
 */
public class LdapHelper {

    private LdapHelper() {
    }

    /**
     * Creates a DN instance.
     *
     * @param schemaManager the schema manager
     * @param dnString      the DN string
     * @return the DN
     */
    public static Dn createDn(SchemaManager schemaManager, String dnString) {

        try {

            return new Dn(schemaManager, dnString);

        } catch (LdapInvalidDnException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a DN with suffix.
     *
     * @param schemaManager the schema manager
     * @param suffixDn      the suffix dn
     * @param entryId       the entry ID
     * @return the DN
     */
    public static Dn createDnWithCn(SchemaManager schemaManager, Dn suffixDn, String entryId) {

        return createDn(schemaManager, String.format("cn=%s,%s", entryId, suffixDn.getName()));
    }

    /**
     * Creates a root DN.
     *
     * @param schemaManager the schema manager
     * @param id            the DC ID
     * @return the DN
     */
    public static Dn createRootDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "dc=" + id.toLowerCase());
    }

    /**
     * Creates groups DN.
     *
     * @param schemaManager the schema manager
     * @param id            the group ID
     * @return the DN
     */
    public static Dn createGroupsDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "ou=" + Utils.OU_GROUPS + ",dc=" + id.toLowerCase());
    }

    /**
     * Creates users DN.
     *
     * @param schemaManager the schema manager
     * @param id            the user ID
     * @return the DN
     */
    public static Dn createUsersDn(SchemaManager schemaManager, String id) {

        return createDn(schemaManager, "ou=" + Utils.OU_USERS + ",dc=" + id.toLowerCase());
    }

    /**
     * Gets group from DN.
     *
     * @param rootDn  the root DN
     * @param groupDn the group DN
     * @param queryDn the query DN
     * @return the group
     */
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

    /**
     * Gets user from DN.
     *
     * @param rootDn  the root DN
     * @param userDn  the user DN
     * @param queryDn the query DN
     * @return the user
     */
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
