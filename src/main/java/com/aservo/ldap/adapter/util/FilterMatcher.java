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

import com.google.common.collect.Sets;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.filter.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The handler for filter expressions.
 */
public abstract class FilterMatcher {

    private final Logger logger = LoggerFactory.getLogger(FilterMatcher.class);

    /**
     * Gets a list of attribute maps. This result can be used to transform a directory backend call in
     * multiple lightweight calls.
     * <p>
     * This method allows performance optimization by reducing heavy directory backend calls. Instead of loading all
     * entities and then filtering them, required entities can be determined by a filter expression.
     * </p>
     * <p>
     * The goal is to split queries in a way that allows to merge the results with an union operation together without
     * loss of information. However, duplicates are allowed.<br>
     * 1) distributive property for intersection operation with outer expression and partial expression<br>
     * <pre>
     *   A ∩ ⋃B, i∊I
     *   ⟹ x∊A ∧ ∃i∊I:x∊B
     *   ⟹ x∊A ∧ ∃i:(x∊I ∧ x∊B)
     *   ⟹ ∃i:(x∊A ∧ (i∊I ∧ x∊B))
     *   ⟹ ∃i∊I:(x∊A ∧ x∊B)
     *   ⟹ ⋃(A ∩ B)
     * </pre>
     * 2) algorithm<br>
     * 2.a) intersection operations as cartesian product<br>
     * <pre>
     *   FROM:
     *   P1∊(A ∩ B1)
     *   P2∊(A ∩ B2)
     *   ...
     *   Pn∊(A ∩ Bn)
     *   TO:
     *   (P1, P2, ..., Pn)∊( (A ∩ B1) × (A ∩ B2) × ... × (A ∩ Bn) )
     * </pre>
     * 2.b) union operations as set of sets<br>
     * <pre>
     *   FROM:
     *   P1 ∪ P1 ∪ ... ∪ Pn
     *   TO:
     *   {P1, P2, ..., Pn}
     * </pre>
     * 2.c) simplification of tuples with multiple entries<br>
     * <pre>
     *   (a, b, c) := ((a, b), c)
     *   (a, b, c, d) := ((a, b, c), d)
     * </pre>
     * 2.d) steps<br>
     * 1. replace boolean expression from filter with rules 2.a and 2.b<br>
     * 2. calculate the first most inner cartesian product<br>
     * 3. simplify tuples with rule 2.c<br>
     * 4. repeat from step 2 until no cartesian product operation is left<br>
     * </p>
     *
     * @param filter    the filter expression
     * @param entryType the entry type
     * @return the attribute maps
     */
    public List<Map<String, String>> getAttributeMaps(ExprNode filter, EntryType entryType) {

        if (filter instanceof AndNode) {

            AndNode node = (AndNode) filter;
            List<Map<String, String>> resultList = new ArrayList<>();

            List<List<Map<String, String>>> superStructure =
                    node.getChildren().stream()
                            .map(x -> getAttributeMaps(x, entryType))
                            .collect(Collectors.toList());

            superStructure.removeIf(List::isEmpty);

            for (List<Map<String, String>> tuples : Utils.cartesianProduct(superStructure)) {

                Map<String, String> tupleFlattened = new HashMap<>();

                for (Map<String, String> tuple : tuples) {

                    if (!Sets.intersection(tuple.keySet(), tupleFlattened.entrySet()).isEmpty())
                        return Collections.emptyList();

                    tupleFlattened.putAll(tuple);
                }

                resultList.add(tupleFlattened);
            }

            return resultList;

        } else if (filter instanceof OrNode) {

            OrNode node = (OrNode) filter;

            List<Map<String, String>> resultList =
                    node.getChildren().stream()
                            .flatMap(x -> getAttributeMaps(x, entryType).stream())
                            .collect(Collectors.toList());

            return resultList;

        } else if (filter instanceof EqualityNode) {

            EqualityNode node = (EqualityNode) filter;
            Map<String, String> map = new HashMap<>();

            switch (Utils.normalizeAttribute(node.getAttribute())) {

                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    if (entryType.equals(EntryType.GROUP))
                        map.put(DirectoryBackend.GROUP_ID, node.getValue().toString());
                    else if (entryType.equals(EntryType.USER))
                        map.put(DirectoryBackend.USER_ID, node.getValue().toString());

                    break;

                case SchemaConstants.GN_AT:
                case SchemaConstants.GN_AT_OID:
                case SchemaConstants.GIVENNAME_AT:

                    if (entryType.equals(EntryType.USER))
                        map.put(DirectoryBackend.USER_FIRST_NAME, node.getValue().toString());

                    break;

                case SchemaConstants.SN_AT:
                case SchemaConstants.SN_AT_OID:
                case SchemaConstants.SURNAME_AT:

                    if (entryType.equals(EntryType.USER))
                        map.put(DirectoryBackend.USER_LAST_NAME, node.getValue().toString());

                    break;

                case SchemaConstants.DISPLAY_NAME_AT:
                case SchemaConstants.DISPLAY_NAME_AT_OID:

                    if (entryType.equals(EntryType.USER))
                        map.put(DirectoryBackend.USER_DISPLAY_NAME, node.getValue().toString());

                    break;

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    if (entryType.equals(EntryType.USER))
                        map.put(DirectoryBackend.USER_EMAIL_ADDRESS, node.getValue().toString());

                    break;

                case SchemaConstants.DESCRIPTION_AT:
                case SchemaConstants.DESCRIPTION_AT_OID:

                    if (entryType.equals(EntryType.GROUP))
                        map.put(DirectoryBackend.GROUP_DESCRIPTION, node.getValue().toString());

                    break;

                default:
                    break;
            }

            if (map.isEmpty())
                return Collections.emptyList();

            return Utils.nullableSingletonList(map);
        }

        return Collections.emptyList();
    }

    /**
     * Make a match between entry and filter expression.
     *
     * @param filter    the filter expression
     * @param entryId   the entry ID
     * @param entryType the entry type
     * @return the boolean
     */
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
                        compare.test(value, Utils.nullableSingletonList(entryId)))
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

    /**
     * Gets related values from attribute.
     *
     * @param attribute the attribute
     * @param entryId   the entry ID
     * @param entryType the entry type
     * @return the values from attribute
     */
    protected abstract List<String> getValuesFromAttribute(String attribute, String entryId, EntryType entryType);

    /**
     * Gets group from DN.
     *
     * @param value the value
     * @return the group from dn
     */
    @Nullable
    protected abstract String getGroupFromDn(@Nullable String value);

    /**
     * Gets user from DN.
     *
     * @param value the value
     * @return the user from dn
     */
    @Nullable
    protected abstract String getUserFromDn(@Nullable String value);
}
