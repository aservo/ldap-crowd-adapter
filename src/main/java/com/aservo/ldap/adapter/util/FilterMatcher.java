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
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.filter.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class FilterMatcher {

    private final Logger logger = LoggerFactory.getLogger(FilterMatcher.class);

    public boolean match(ExprNode filter, String entryId, EntryType entryType) {

        if (filter instanceof ObjectClassNode) {

            return true;

        } else if (filter instanceof AndNode) {

            AndNode node = (AndNode) filter;

            return node.getChildren().stream()
                    .allMatch(x -> match(x, entryId, entryType));

        } else if (filter instanceof OrNode) {

            OrNode node = (OrNode) filter;

            return node.getChildren().stream()
                    .anyMatch(x -> match(x, entryId, entryType));

        } else if (filter instanceof NotNode) {

            NotNode node = (NotNode) filter;

            return node.getChildren().stream()
                    .noneMatch(x -> match(x, entryId, entryType));

        } else if (filter instanceof EqualityNode) {

            EqualityNode node = (EqualityNode) filter;

            return matchPairs(node.getAttribute(), node.getValue().toString(), entryId, entryType, this::compareEquality);

        } else if (filter instanceof PresenceNode) {

            PresenceNode node = (PresenceNode) filter;

            return matchPairs(node.getAttribute(), null, entryId, entryType, this::comparePresence);
        }

        logger.warn("Cannot process unsupported filter node {}", filter.getClass().getName());
        return false;
    }

    private boolean matchPairs(String attribute,
                               @Nullable String value,
                               String entryId,
                               EntryType entryType,
                               BiPredicate<String, List<String>> compare) {

        switch (Utils.normalizeAttribute(attribute)) {

            case SchemaConstants.OBJECT_CLASS_AT:
            case SchemaConstants.OBJECT_CLASS_AT_OID:

                if (entryType.equals(EntryType.DOMAIN) &&
                        compare.test(value, Arrays.asList(
                                SchemaConstants.DOMAIN_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                if (entryType.equals(EntryType.UNIT) &&
                        compare.test(value, Arrays.asList(
                                SchemaConstants.ORGANIZATIONAL_UNIT_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                if (entryType.equals(EntryType.GROUP) &&
                        compare.test(value, Arrays.asList(
                                SchemaConstants.GROUP_OF_NAMES_OC,
                                SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, Arrays.asList(
                                SchemaConstants.INET_ORG_PERSON_OC,
                                SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                                SchemaConstants.PERSON_OC,
                                SchemaConstants.TOP_OC)))
                    return true;

                break;

            case SchemaConstants.UID_NUMBER_AT:
            case SchemaConstants.UID_NUMBER_AT_OID:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value,
                                Utils.nullableSingletonList(Integer.toString(Utils.calculateHash(entryId)))))
                    return true;

                break;

            case SchemaConstants.UID_AT:
            case SchemaConstants.UID_AT_OID:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, Utils.nullableSingletonList(entryId)))
                    return true;

                break;

            case SchemaConstants.OU_AT:
            case SchemaConstants.OU_AT_OID:

                if (entryType.equals(EntryType.UNIT) &&
                        compare.test(value, Utils.nullableSingletonList(entryId)))
                    return true;

                if (entryType.equals(EntryType.GROUP) &&
                        compare.test(value, Utils.nullableSingletonList(Utils.OU_GROUPS)))
                    return true;

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, Utils.nullableSingletonList(Utils.OU_USERS)))
                    return true;

                break;

            case SchemaConstants.DC_AT:
            case SchemaConstants.DOMAIN_COMPONENT_AT:
            case SchemaConstants.DOMAIN_COMPONENT_AT_OID:

                if (entryType.equals(EntryType.DOMAIN) &&
                        compare.test(value, Utils.nullableSingletonList(entryId)))
                    return true;

                break;

            case SchemaConstants.CN_AT:
            case SchemaConstants.CN_AT_OID:
            case SchemaConstants.COMMON_NAME_AT:

                if ((entryType.equals(EntryType.GROUP) || entryType.equals(EntryType.USER)) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.GN_AT:
            case SchemaConstants.GN_AT_OID:
            case SchemaConstants.GIVENNAME_AT:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.SN_AT:
            case SchemaConstants.SN_AT_OID:
            case SchemaConstants.SURNAME_AT:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.DISPLAY_NAME_AT:
            case SchemaConstants.DISPLAY_NAME_AT_OID:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.MAIL_AT:
            case SchemaConstants.MAIL_AT_OID:

                if (entryType.equals(EntryType.USER) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entryType.equals(EntryType.DOMAIN) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                if (entryType.equals(EntryType.UNIT) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                if (entryType.equals(EntryType.GROUP) &&
                        compare.test(value, getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            case SchemaConstants.MEMBER_AT:
            case SchemaConstants.MEMBER_AT_OID:
            case SchemaConstants.UNIQUE_MEMBER_AT:
            case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                if (entryType.equals(EntryType.GROUP) &&
                        compare.test(getUserFromDn(value),
                                getValuesFromAttribute(attribute, entryId, entryType).stream()
                                        .map(this::getUserFromDn)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())))
                    return true;

                if (entryType.equals(EntryType.GROUP) &&
                        compare.test(getGroupFromDn(value),
                                getValuesFromAttribute(attribute, entryId, entryType).stream()
                                        .map(this::getGroupFromDn)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())))
                    return true;

                break;

            case Utils.MEMBER_OF_AT:

                if ((entryType.equals(EntryType.GROUP) || entryType.equals(EntryType.USER)) &&
                        compare.test(getGroupFromDn(value), getValuesFromAttribute(attribute, entryId, entryType)))
                    return true;

                break;

            default:
                break;
        }

        return false;
    }

    private boolean compareEquality(@Nullable String value, List<String> alternatives) {

        return value != null && alternatives.stream().anyMatch(x -> x.equalsIgnoreCase(value));
    }

    private boolean comparePresence(@Nullable String value, List<String> alternatives) {

        return value == null && !alternatives.isEmpty();
    }

    protected abstract List<String> getValuesFromAttribute(String attribute, String entryId, EntryType entryType);

    @Nullable
    protected abstract String getGroupFromDn(@Nullable String value);

    @Nullable
    protected abstract String getUserFromDn(@Nullable String value);
}
