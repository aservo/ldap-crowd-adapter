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
import com.aservo.ldap.adapter.adapter.entity.Entity;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.LruCacheMap;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A proxy for directory backends to cache entities.
 */
public class CachedDirectoryBackend
        implements DirectoryBackend {

    private final DirectoryBackend directoryBackend;
    private final Map<String, GroupEntity> groupIdToEntityCache;
    private final Map<String, UserEntity> userIdToEntityCache;
    private final Map<FilterNode, Set<String>> groupFilterToIdCache;
    private final Map<FilterNode, Set<String>> userFilterToIdCache;
    private final Map<String, Set<String>> directGroupsOfUserCache;
    private final Map<String, Set<String>> directUsersOfGroupCache;
    private final Map<String, Set<String>> transitiveUsersOfGroupCache;
    private final Map<String, Set<String>> transitiveGroupsOfUserCache;
    private final Map<String, Set<String>> directChildGroupsOfGroupCache;
    private final Map<String, Set<String>> directParentGroupsOfGroupCache;
    private final Map<String, Set<String>> transitiveChildGroupsOfGroupCache;
    private final Map<String, Set<String>> transitiveParentGroupsOfGroupCache;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param directoryBackend the directory backend
     * @param maxCacheSize     the maximum cache size
     * @param maxCacheAge      the maximum cache age
     */
    public CachedDirectoryBackend(DirectoryBackend directoryBackend, int maxCacheSize, Duration maxCacheAge) {

        this.directoryBackend = directoryBackend;

        this.groupIdToEntityCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.userIdToEntityCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.groupFilterToIdCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.userFilterToIdCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.directGroupsOfUserCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.directUsersOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.transitiveUsersOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.transitiveGroupsOfUserCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.directChildGroupsOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.directParentGroupsOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.transitiveChildGroupsOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
        this.transitiveParentGroupsOfGroupCache = Collections.synchronizedMap(new LruCacheMap<>(maxCacheSize, maxCacheAge));
    }

    public String getId() {

        return directoryBackend.getId();
    }

    public void startup() {

        directoryBackend.startup();
    }

    public void shutdown() {

        directoryBackend.shutdown();

        groupIdToEntityCache.clear();
        userIdToEntityCache.clear();
        groupFilterToIdCache.clear();
        userFilterToIdCache.clear();
        directGroupsOfUserCache.clear();
        directUsersOfGroupCache.clear();
    }

    public boolean isKnownGroup(String id) {

        return groupIdToEntityCache.containsKey(id) || directoryBackend.isKnownGroup(id);
    }

    public boolean isKnownUser(String id) {

        return userIdToEntityCache.containsKey(id) || directoryBackend.isKnownUser(id);
    }

    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        GroupEntity group = groupIdToEntityCache.get(id);

        if (group != null)
            return group;

        GroupEntity result = directoryBackend.getGroup(id);

        groupIdToEntityCache.put(id, result);

        return result;
    }

    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        UserEntity user = userIdToEntityCache.get(id);

        if (user != null)
            return user;

        UserEntity result = directoryBackend.getUser(id);

        userIdToEntityCache.put(id, result);

        return result;
    }

    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        return directoryBackend.getAuthenticatedUser(id, password);
    }

    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        List<GroupEntity> groups = lookupGroupEntities(groupFilterToIdCache, filterNode);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getGroups(filterNode, filterMatcher);

        saveGroupEntities(groupFilterToIdCache, filterNode, result);

        return result;
    }

    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        List<UserEntity> users = lookupUserEntities(userFilterToIdCache, filterNode);

        if (users != null)
            return users;

        List<UserEntity> result = directoryBackend.getUsers(filterNode, filterMatcher);

        saveUserEntities(userFilterToIdCache, filterNode, result);

        return result;
    }

    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        List<UserEntity> users = lookupUserEntities(directUsersOfGroupCache, id);

        if (users != null)
            return users;

        List<UserEntity> result = directoryBackend.getDirectUsersOfGroup(id);

        saveUserEntities(directUsersOfGroupCache, id, result);

        return result;
    }

    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directGroupsOfUserCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectGroupsOfUser(id);

        saveGroupEntities(directGroupsOfUserCache, id, result);

        return result;
    }

    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        List<UserEntity> users = lookupUserEntities(transitiveUsersOfGroupCache, id);

        if (users != null)
            return users;

        List<UserEntity> result = directoryBackend.getTransitiveUsersOfGroup(id);

        saveUserEntities(transitiveUsersOfGroupCache, id, result);

        return result;
    }

    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(transitiveGroupsOfUserCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getTransitiveGroupsOfUser(id);

        saveGroupEntities(transitiveGroupsOfUserCache, id, result);

        return result;
    }

    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directChildGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectChildGroupsOfGroup(id);

        saveGroupEntities(directChildGroupsOfGroupCache, id, result);

        return result;
    }

    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directParentGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectParentGroupsOfGroup(id);

        saveGroupEntities(directParentGroupsOfGroupCache, id, result);

        return result;
    }

    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(transitiveChildGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getTransitiveChildGroupsOfGroup(id);

        saveGroupEntities(transitiveChildGroupsOfGroupCache, id, result);

        return result;
    }

    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(transitiveParentGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getTransitiveParentGroupsOfGroup(id);

        saveGroupEntities(transitiveParentGroupsOfGroupCache, id, result);

        return result;
    }

    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> userIds = directUsersOfGroupCache.get(id);

        if (userIds != null)
            return Lists.newArrayList(userIds);

        List<String> result = directoryBackend.getDirectUserIdsOfGroup(id);

        saveEntityIds(directUsersOfGroupCache, id, result);

        return result;
    }

    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directGroupsOfUserCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectGroupIdsOfUser(id);

        saveEntityIds(directGroupsOfUserCache, id, result);

        return result;
    }

    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> userIds = transitiveUsersOfGroupCache.get(id);

        if (userIds != null)
            return Lists.newArrayList(userIds);

        List<String> result = directoryBackend.getTransitiveUserIdsOfGroup(id);

        saveEntityIds(transitiveUsersOfGroupCache, id, result);

        return result;
    }

    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = transitiveGroupsOfUserCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getTransitiveGroupIdsOfUser(id);

        saveEntityIds(transitiveGroupsOfUserCache, id, result);

        return result;
    }

    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directChildGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectChildGroupIdsOfGroup(id);

        saveEntityIds(directChildGroupsOfGroupCache, id, result);

        return result;
    }

    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directParentGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectParentGroupIdsOfGroup(id);

        saveEntityIds(directParentGroupsOfGroupCache, id, result);

        return result;
    }

    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = transitiveChildGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getTransitiveChildGroupIdsOfGroup(id);

        saveEntityIds(transitiveChildGroupsOfGroupCache, id, result);

        return result;
    }

    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = transitiveParentGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getTransitiveParentGroupIdsOfGroup(id);

        saveEntityIds(transitiveParentGroupsOfGroupCache, id, result);

        return result;
    }

    private <T> List<GroupEntity> lookupGroupEntities(Map<T, Set<String>> cache, T id) {

        Set<String> groupIds = cache.get(id);

        if (groupIds != null) {

            List<GroupEntity> groups = groupIds.stream()
                    .map(groupIdToEntityCache::get)
                    .collect(Collectors.toList());

            if (groups.stream().allMatch(Objects::nonNull))
                return groups;
        }

        return null;
    }

    private <T> List<UserEntity> lookupUserEntities(Map<T, Set<String>> cache, T id) {

        Set<String> userIds = cache.get(id);

        if (userIds != null) {

            List<UserEntity> users = userIds.stream()
                    .map(userIdToEntityCache::get)
                    .collect(Collectors.toList());

            if (users.stream().allMatch(Objects::nonNull))
                return users;
        }

        return null;
    }

    private <T> void saveGroupEntities(Map<T, Set<String>> cache, T id, List<GroupEntity> groups) {

        groups.forEach(x -> groupIdToEntityCache.put(x.getId(), x));
        cache.put(id, groups.stream().map(Entity::getId).collect(Collectors.toSet()));
    }

    private <T> void saveUserEntities(Map<T, Set<String>> cache, T id, List<UserEntity> users) {

        users.forEach(x -> userIdToEntityCache.put(x.getId(), x));
        cache.put(id, users.stream().map(Entity::getId).collect(Collectors.toSet()));
    }

    private <T> void saveEntityIds(Map<T, Set<String>> cache, T id, List<String> groups) {

        cache.put(id, new HashSet<>(groups));
    }
}
