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
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.util.List;
import java.util.Optional;


public abstract class ProxyDirectoryBackend
        implements DirectoryBackend {

    protected final ServerConfiguration config;
    protected final DirectoryBackend directoryBackend;

    protected ProxyDirectoryBackend(ServerConfiguration config, DirectoryBackend directoryBackend) {

        this.config = config;
        this.directoryBackend = directoryBackend;
    }

    @Override
    public String getId() {

        return directoryBackend.getId();
    }

    @Override
    public void startup() {

        directoryBackend.startup();
    }

    @Override
    public void shutdown() {

        directoryBackend.shutdown();
    }

    @Override
    public boolean isKnownGroup(String id) {

        return directoryBackend.isKnownGroup(id);
    }

    @Override
    public boolean isKnownUser(String id) {

        return directoryBackend.isKnownUser(id);
    }

    @Override
    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getGroup(id);
    }

    @Override
    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getUser(id);
    }

    @Override
    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        return directoryBackend.getAuthenticatedUser(id, password);
    }

    @Override
    public List<GroupEntity> getAllGroups() {

        return directoryBackend.getAllGroups();
    }

    @Override
    public List<UserEntity> getAllUsers() {

        return directoryBackend.getAllUsers();
    }

    @Override
    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        return directoryBackend.getGroups(filterNode, filterMatcher);
    }

    @Override
    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        return directoryBackend.getUsers(filterNode, filterMatcher);
    }

    @Override
    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectUsersOfGroup(id);
    }

    @Override
    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectGroupsOfUser(id);
    }

    @Override
    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveUsersOfGroup(id);
    }

    @Override
    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveGroupsOfUser(id);
    }

    @Override
    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectChildGroupsOfGroup(id);
    }

    @Override
    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectParentGroupsOfGroup(id);
    }

    @Override
    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveChildGroupsOfGroup(id);
    }

    @Override
    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveParentGroupsOfGroup(id);
    }

    @Override
    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectUserIdsOfGroup(id);
    }

    @Override
    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectGroupIdsOfUser(id);
    }

    @Override
    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveUserIdsOfGroup(id);
    }

    @Override
    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveGroupIdsOfUser(id);
    }

    @Override
    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectChildGroupIdsOfGroup(id);
    }

    @Override
    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getDirectParentGroupIdsOfGroup(id);
    }

    @Override
    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveChildGroupIdsOfGroup(id);
    }

    @Override
    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return directoryBackend.getTransitiveParentGroupIdsOfGroup(id);
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        return directoryBackend.isGroupDirectGroupMember(groupId1, groupId2);
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        return directoryBackend.isUserDirectGroupMember(userId, groupId);
    }

    @Override
    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2) {

        return directoryBackend.isGroupTransitiveGroupMember(groupId1, groupId2);
    }

    @Override
    public boolean isUserTransitiveGroupMember(String userId, String groupId) {

        return directoryBackend.isUserTransitiveGroupMember(userId, groupId);
    }
}
