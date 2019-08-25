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

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.filter.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class FilterMatcher {

    private final Logger logger = LoggerFactory.getLogger(FilterMatcher.class);

    public boolean match(ExprNode filter, String entryId, OuType ouType) {

        if (filter instanceof ObjectClassNode) {

            return true;

        } else if (filter instanceof AndNode) {

            AndNode node = (AndNode) filter;

            return node.getChildren().stream()
                    .allMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof OrNode) {

            OrNode node = (OrNode) filter;

            return node.getChildren().stream()
                    .anyMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof NotNode) {

            NotNode node = (NotNode) filter;

            return node.getChildren().stream()
                    .noneMatch((x) -> match(x, entryId, ouType));

        } else if (filter instanceof EqualityNode) {

            EqualityNode node = (EqualityNode) filter;

            return matchPairs(node.getAttribute(), node.getValue().toString(), entryId, ouType, this::compareEquality);

        } else if (filter instanceof PresenceNode) {

            PresenceNode node = (PresenceNode) filter;

            return matchPairs(node.getAttribute(), null, entryId, ouType, this::comparePresence);
        }

        logger.warn("Cannot process unsupported filter node " + filter.getClass().getName());
        return false;
    }

    private boolean matchPairs(String attribute,
                               @Nullable String value,
                               String entryId,
                               OuType ouType,
                               BiFunction<String, List<String>, Boolean> compare) {

        switch (Utils.normalizeAttribute(attribute)) {

            case SchemaConstants.OBJECT_CLASS_AT:
            case SchemaConstants.OBJECT_CLASS_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, Arrays.asList(
                                SchemaConstants.GROUP_OF_NAMES_OC,
                                SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Arrays.asList(
                                SchemaConstants.INET_ORG_PERSON_OC,
                                SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                                SchemaConstants.PERSON_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                break;

            case SchemaConstants.UID_NUMBER_AT:
            case SchemaConstants.UID_NUMBER_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Utils.nullableSingletonList(Utils.calculateHash(entryId).toString())))
                    return true;

                break;

            case SchemaConstants.UID_AT:
            case SchemaConstants.UID_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Utils.nullableSingletonList(entryId)))
                    return true;

                break;

            case SchemaConstants.OU_AT:
            case SchemaConstants.OU_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, Utils.nullableSingletonList(Utils.OU_GROUPS)))
                    return true;

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, Utils.nullableSingletonList(Utils.OU_USERS)))
                    return true;

                break;

            case SchemaConstants.CN_AT:
            case SchemaConstants.CN_AT_OID:
            case SchemaConstants.COMMON_NAME_AT:

                if ((ouType.equals(OuType.GROUP) || ouType.equals(OuType.USER)) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.GN_AT:
            case SchemaConstants.GN_AT_OID:
            case SchemaConstants.GIVENNAME_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.SN_AT:
            case SchemaConstants.SN_AT_OID:
            case SchemaConstants.SURNAME_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.DISPLAY_NAME_AT:
            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.MAIL_AT:
            case SchemaConstants.MAIL_AT_OID:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(value, getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case SchemaConstants.MEMBER_AT:
            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (ouType.equals(OuType.GROUP) &&
                        compare.apply(getUserFromDn(value), getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            case Utils.MEMBER_OF_AT:

                if (ouType.equals(OuType.USER) &&
                        compare.apply(getGroupFromDn(value), getValuesFromAttribute(attribute, entryId, ouType)))
                    return true;

                break;

            default:
                break;
        }

        return false;
    }

    private boolean compareEquality(@Nullable String value, List<String> alternatives) {

        return value != null && alternatives.stream().anyMatch((x) -> x.equalsIgnoreCase(value));
    }

    private boolean comparePresence(@Nullable String value, List<String> alternatives) {

        return value == null && !alternatives.isEmpty();
    }

    protected abstract List<String> getValuesFromAttribute(String attribute, String entryId, OuType ouType);

    @Nullable
    protected abstract String getGroupFromDn(@Nullable String value);

    @Nullable
    protected abstract String getUserFromDn(@Nullable String value);
}
