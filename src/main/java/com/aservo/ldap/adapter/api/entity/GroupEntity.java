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

package com.aservo.ldap.adapter.api.entity;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The group entity.
 */
public class GroupEntity
        extends Entity
        implements DescribableEntity {

    private final String name;
    private final String description;
    private final Set<String> memberOfNames = new HashSet<>();
    private final Set<String> memberNamesGroup = new HashSet<>();
    private final Set<String> memberNamesUser = new HashSet<>();

    /**
     * Instantiates a new Group.
     *
     * @param name             the name
     * @param description      the description
     * @param memberOfNames    the names for memberOf attribute
     * @param memberNamesGroup the names for group member attribute
     * @param memberNamesUser  the names for user member attribute
     */
    public GroupEntity(String name, String description, Collection<String> memberOfNames,
                       Collection<String> memberNamesGroup, Collection<String> memberNamesUser) {

        super(name.toLowerCase());
        this.name = name;
        this.description = description;
        this.memberOfNames.addAll(memberOfNames);
        this.memberNamesGroup.addAll(memberNamesGroup);
        this.memberNamesUser.addAll(memberNamesUser);
    }

    /**
     * Instantiates a new Group.
     *
     * @param name        the name
     * @param description the description
     */
    public GroupEntity(String name, String description) {

        super(name.toLowerCase());
        this.name = name;
        this.description = description;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {

        return name;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {

        return description;
    }

    /**
     * Gets names for memberOf attribute.
     *
     * @return the description
     */
    public Iterable<String> getMemberOfNames() {

        return memberOfNames;
    }

    /**
     * Gets names for group member attribute.
     *
     * @return the description
     */
    public Iterable<String> getMemberNamesGroup() {

        return memberNamesGroup;
    }

    /**
     * Gets names for user member attribute.
     *
     * @return the description
     */
    public Iterable<String> getMemberNamesUser() {

        return memberNamesUser;
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public EntityType getEntityType() {

        return EntityType.GROUP;
    }
}
