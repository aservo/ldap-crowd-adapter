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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.jetbrains.annotations.Nullable;


/**
 * A helper for misc things.
 */
public class Utils {

    /**
     * The constant OU_GROUPS.
     */
    public static final String OU_GROUPS = "groups";
    /**
     * The constant OU_USERS.
     */
    public static final String OU_USERS = "users";

    /**
     * The constant MEMBER_OF_AT.
     */
    public static final String MEMBER_OF_AT = "memberOf";

    private Utils() {
    }

    /**
     * Calculates a hash value from string.
     *
     * @param value the value
     * @return the int
     */
    public static int calculateHash(@Nullable String value) {

        if (value == null)
            return 0;

        int hash = value.hashCode();

        if (hash < 0)
            hash *= 31;
        else
            hash *= 13;

        return (Math.abs(hash) % 9999999) + 1;
    }

    /**
     * Creates a list with one or zero elements.
     * To add is not allowed.
     *
     * @param <T>   the type parameter
     * @param value the value
     * @return the list
     */
    public static <T> List<T> nullableSingletonList(T value) {

        if (value == null)
            return Collections.emptyList();

        return Collections.singletonList(value);
    }

    /**
     * Creates a list with one or zero elements.
     * To add is allowed.
     *
     * @param <T>   the type parameter
     * @param value the value
     * @return the list
     */
    public static <T> List<T> nullableOneElementList(T value) {

        List<T> list = new ArrayList<>();

        if (value == null)
            return list;

        list.add(value);

        return list;
    }

    /**
     * Creates the cartesian product.
     *
     * @param <T>   the type parameter
     * @param lists the input structure
     * @return the output structure
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {

        List<List<T>> resultLists = new ArrayList<>();

        if (lists.isEmpty()) {

            resultLists.add(new ArrayList<>());

        } else {

            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));

            for (T value : firstList)
                for (List<T> remainingList : remainingLists) {

                    ArrayList<T> resultList = new ArrayList<T>();

                    resultList.add(value);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
        }

        return resultLists;
    }

    /**
     * Checks the equality of two maps.
     *
     * @param <K>  the type parameter for keys
     * @param <V>  the type parameter for values
     * @param map1 the first map structure
     * @param map2 the second map structure
     * @return the boolean value for equality
     */
    public static <K, V> boolean mapsEqual(Map<K, V> map1, Map<K, V> map2) {

        if (map1.size() != map2.size())
            return false;

        return map1.entrySet().stream().allMatch(e -> e.getValue().equals(map2.get(e.getKey())));
    }

    /**
     * Creates a distinct list of maps.
     *
     * @param <K>   the type parameter for keys
     * @param <V>   the type parameter for values
     * @param input the input maps to make distinct
     * @return the output maps which are distinct
     */
    public static <K, V> List<Map<K, V>> createDistinctMaps(List<Map<K, V>> input) {

        List<Map<K, V>> output = new ArrayList<>();

        for (Map<K, V> map1 : input) {

            boolean found = false;

            for (Map<K, V> map2 : output) {

                if (mapsEqual(map1, map2))
                    found = true;
            }

            if (!found)
                output.add(map1);
        }

        return output;
    }

    /**
     * Normalizes LDAP attributes.
     * Function is used for attributes of incoming queries.
     *
     * @param attribute the attribute
     * @return the normalized attribute
     */
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
