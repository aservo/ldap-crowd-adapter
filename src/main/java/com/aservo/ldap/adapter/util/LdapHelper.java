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
import org.apache.directory.api.ldap.model.name.Dn;
import org.jetbrains.annotations.Nullable;


public class LdapHelper {

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
