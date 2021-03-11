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

package com.aservo.ldap.adapter.backend;

import com.aservo.ldap.adapter.adapter.FilterMatcher;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.adapter.query.NullNode;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import java.util.List;
import java.util.Optional;


/**
 * The interface for all directory backends.
 */
public interface DirectoryBackend {

    /**
     * Gets backend ID.
     *
     * @return the backend ID
     */
    String getId();

    /**
     * Startup method.
     */
    void startup();

    /**
     * Shutdown method.
     */
    void shutdown();

    /**
     * Gets group info.
     *
     * @param id the group ID
     * @return the group
     * @throws EntityNotFoundException the entry not found exception
     */
    GroupEntity getGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets user info.
     *
     * @param id the user ID
     * @return the user
     * @throws EntityNotFoundException the entry not found exception
     */
    UserEntity getUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets info from authenticated user.
     *
     * @param id       the user ID
     * @param password the password
     * @return the authenticated user
     * @throws EntityNotFoundException the entry not found exception
     */
    UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException;

    /**
     * Gets all groups.
     *
     * @return the groups
     */
    default List<GroupEntity> getAllGroups() {

        return getGroups(new NullNode(), Optional.empty());
    }

    /**
     * Gets all users.
     *
     * @return the users
     */
    default List<UserEntity> getAllUsers() {

        return getUsers(new NullNode(), Optional.empty());
    }

    /**
     * Gets groups by filter expression.
     *
     * @param filterNode    the filter expression node
     * @param filterMatcher the optional filter matcher
     * @return the filtered groups
     */
    List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher);

    /**
     * Gets users by filter expression.
     *
     * @param filterNode    the filter expression node
     * @param filterMatcher the optional filter matcher
     * @return the filtered users
     */
    List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher);

    /**
     * Gets direct users of group.
     *
     * @param id the group ID
     * @return the direct users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct groups of user.
     *
     * @param id the user ID
     * @return the direct groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive users of group.
     *
     * @param id the group ID
     * @return the transitive users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive groups of user.
     *
     * @param id the user ID
     * @return the transitive groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct child groups of group.
     *
     * @param id the group ID
     * @return the direct child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct parent groups of group.
     *
     * @param id the group ID
     * @return the direct parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive child groups of group.
     *
     * @param id the group ID
     * @return the transitive child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive parent groups of group.
     *
     * @param id the group ID
     * @return the transitive parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct user IDs of group.
     *
     * @param id the group ID
     * @return the direct users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct group IDs of user.
     *
     * @param id the user ID
     * @return the direct groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive user IDs of group.
     *
     * @param id the group ID
     * @return the transitive users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive group IDs of user.
     *
     * @param id the user ID
     * @return the transitive groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct child group IDs of group.
     *
     * @param id the group ID
     * @return the direct child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct parent group IDs of group.
     *
     * @param id the group ID
     * @return the direct parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive child group IDs of group.
     *
     * @param id the group ID
     * @return the transitive child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive parent group IDs of group.
     *
     * @param id the group ID
     * @return the transitive parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Indicates whether the group is direct group member.
     *
     * @param groupId1 the group ID of child group
     * @param groupId2 the group ID of parent group
     * @return the boolean
     */
    default boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        try {

            return getDirectChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    /**
     * Indicates whether the user is direct group member.
     *
     * @param userId  the user ID
     * @param groupId the group ID
     * @return the boolean
     */
    default boolean isUserDirectGroupMember(String userId, String groupId) {

        try {

            return getDirectUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    /**
     * Indicates whether the group is transitive group member.
     *
     * @param groupId1 the group ID of child group
     * @param groupId2 the group ID of parent group
     * @return the boolean
     */
    default boolean isGroupTransitiveGroupMember(String groupId1, String groupId2) {

        try {

            return getTransitiveChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    /**
     * Indicates whether the user is transitive group member.
     *
     * @param userId  the user ID
     * @param groupId the group ID
     * @return the boolean
     */
    default boolean isUserTransitiveGroupMember(String userId, String groupId) {

        try {

            return getTransitiveUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }
}
