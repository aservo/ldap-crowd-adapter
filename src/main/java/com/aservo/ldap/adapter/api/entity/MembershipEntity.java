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

import java.util.HashSet;
import java.util.Set;


/**
 * The membership entity.
 */
public class MembershipEntity
        extends Entity {

    private final Set<String> memberGroupIds;
    private final Set<String> memberUserIds;

    /**
     * Instantiates a new User.
     *
     * @param parentGroupId  the ID of the parent group
     * @param memberGroupIds the IDs of the group memberships
     * @param memberUserIds  the IDs of the user memberships
     */
    public MembershipEntity(String parentGroupId, Set<String> memberGroupIds, Set<String> memberUserIds) {

        super(parentGroupId);
        this.memberGroupIds = new HashSet<>(memberGroupIds);
        this.memberUserIds = new HashSet<>(memberUserIds);
    }

    /**
     * Gets the ID of the parent group.
     *
     * @return the last name
     */
    public String getParentGroupId() {

        return getId();
    }

    /**
     * Gets the IDs of the child groups.
     *
     * @return the display name
     */
    public Set<String> getMemberGroupIds() {

        return new HashSet<>(memberGroupIds);
    }

    /**
     * Gets the IDs of the users.
     *
     * @return the first name
     */
    public Set<String> getMemberUserIds() {

        return new HashSet<>(memberUserIds);
    }

    public EntityType getEntityType() {

        return null;
    }
}
