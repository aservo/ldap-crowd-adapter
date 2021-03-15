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
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.adapter.query.OrLogicExpression;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.LruCacheMap;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import com.google.common.collect.Lists;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;


/**
 * A proxy for directory backends to cache entities via in-memory strategy.
 */
public class CachedInMemoryDirectoryBackend
        extends CachedDirectoryBackend {

    private final Map<String, GroupEntity> groupIdToEntityCache;
    private final Map<String, UserEntity> userIdToEntityCache;
    private final Map<FilterNode, Set<String>> groupFilterToIdCache;
    private final Map<FilterNode, Set<String>> userFilterToIdCache;
    private final Map<String, Set<String>> directGroupsOfUserCache;
    private final Map<String, Set<String>> directUsersOfGroupCache;
    private final Map<String, Set<String>> directChildGroupsOfGroupCache;
    private final Map<String, Set<String>> directParentGroupsOfGroupCache;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param config           config the config instance of the server
     * @param directoryBackend the directory backend
     */
    public CachedInMemoryDirectoryBackend(ServerConfiguration config, DirectoryBackend directoryBackend) {

        super(config, directoryBackend);

        groupIdToEntityCache = createCache();
        userIdToEntityCache = createCache();
        groupFilterToIdCache = createCache();
        userFilterToIdCache = createCache();
        directGroupsOfUserCache = createCache();
        directUsersOfGroupCache = createCache();
        directChildGroupsOfGroupCache = createCache();
        directParentGroupsOfGroupCache = createCache();
    }

    @Override
    public void startup() {

        super.startup();
    }

    @Override
    public void shutdown() {

        super.shutdown();

        groupIdToEntityCache.clear();
        userIdToEntityCache.clear();
        groupFilterToIdCache.clear();
        userFilterToIdCache.clear();
        directGroupsOfUserCache.clear();
        directUsersOfGroupCache.clear();
        directChildGroupsOfGroupCache.clear();
        directParentGroupsOfGroupCache.clear();
    }

    @Override
    public boolean isKnownGroup(String id) {

        return groupIdToEntityCache.containsKey(id) || directoryBackend.isKnownGroup(id);
    }

    @Override
    public boolean isKnownUser(String id) {

        return userIdToEntityCache.containsKey(id) || directoryBackend.isKnownUser(id);
    }

    @Override
    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        GroupEntity group = groupIdToEntityCache.get(id);

        if (group != null)
            return group;

        GroupEntity result = directoryBackend.getGroup(id);

        if (entryCacheEnabled)
            groupIdToEntityCache.put(id, result);

        return result;
    }

    @Override
    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        UserEntity user = userIdToEntityCache.get(id);

        if (user != null)
            return user;

        UserEntity result = directoryBackend.getUser(id);

        if (entryCacheEnabled)
            userIdToEntityCache.put(id, result);

        return result;
    }

    @Override
    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        List<GroupEntity> groups = lookupGroupEntities(groupFilterToIdCache, filterNode);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getGroups(filterNode, filterMatcher);

        if (entryCacheEnabled)
            saveGroupEntities(groupFilterToIdCache, filterNode, new HashSet<>(result));

        return result;
    }

    @Override
    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        List<UserEntity> users = lookupUserEntities(userFilterToIdCache, filterNode);

        if (users != null)
            return users;

        List<UserEntity> result = directoryBackend.getUsers(filterNode, filterMatcher);

        if (entryCacheEnabled)
            saveUserEntities(userFilterToIdCache, filterNode, new HashSet<>(result));

        return result;
    }

    @Override
    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        List<UserEntity> users = lookupUserEntities(directUsersOfGroupCache, id);

        if (users != null)
            return users;

        List<UserEntity> result = directoryBackend.getDirectUsersOfGroup(id);

        if (entryCacheEnabled)
            saveUserEntities(directUsersOfGroupCache, id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directGroupsOfUserCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectGroupsOfUser(id);

        if (entryCacheEnabled)
            saveGroupEntities(directGroupsOfUserCache, id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        List<UserEntity> users = getDirectUsersOfGroup(id);

        for (GroupEntity y : getTransitiveChildGroupsOfGroup(id))
            for (UserEntity x : getDirectUsersOfGroup(y.getId()))
                if (!users.contains(x))
                    users.add(x);

        return users;
    }

    @Override
    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = getDirectGroupsOfUser(id);

        for (GroupEntity y : new ArrayList<>(groups))
            for (GroupEntity x : getTransitiveParentGroupsOfGroup(y.getId()))
                if (!groups.contains(x))
                    groups.add(x);

        return groups;
    }

    @Override
    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directChildGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectChildGroupsOfGroup(id);

        if (entryCacheEnabled)
            saveGroupEntities(directChildGroupsOfGroupCache, id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<GroupEntity> groups = lookupGroupEntities(directParentGroupsOfGroupCache, id);

        if (groups != null)
            return groups;

        List<GroupEntity> result = directoryBackend.getDirectParentGroupsOfGroup(id);

        if (entryCacheEnabled)
            saveGroupEntities(directParentGroupsOfGroupCache, id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> groupIds = getTransitiveChildGroupIdsOfGroup(id);

        if (groupIds.isEmpty())
            return Collections.emptyList();

        return getGroups(new OrLogicExpression(
                groupIds.stream()
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT, x))
                        .collect(Collectors.toList())
        ), Optional.empty());
    }

    @Override
    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> groupIds = getTransitiveParentGroupIdsOfGroup(id);

        if (groupIds.isEmpty())
            return Collections.emptyList();

        return getGroups(new OrLogicExpression(
                groupIds.stream()
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT, x))
                        .collect(Collectors.toList())
        ), Optional.empty());
    }

    @Override
    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> userIds = directUsersOfGroupCache.get(id);

        if (userIds != null)
            return Lists.newArrayList(userIds);

        List<String> result = directoryBackend.getDirectUserIdsOfGroup(id);

        if (entryCacheEnabled)
            directUsersOfGroupCache.put(id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directGroupsOfUserCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectGroupIdsOfUser(id);

        if (entryCacheEnabled)
            directGroupsOfUserCache.put(id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> userIds = getDirectUserIdsOfGroup(id);

        for (String y : getTransitiveChildGroupIdsOfGroup(id))
            for (String x : getDirectUserIdsOfGroup(y))
                if (!userIds.contains(x))
                    userIds.add(x);

        return userIds;
    }

    @Override
    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        List<String> groupIds = getDirectGroupIdsOfUser(id);

        for (String y : new ArrayList<>(groupIds))
            for (String x : getTransitiveParentGroupIdsOfGroup(y))
                if (!groupIds.contains(x))
                    groupIds.add(x);

        return groupIds;
    }

    @Override
    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directChildGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectChildGroupIdsOfGroup(id);

        if (entryCacheEnabled)
            directChildGroupsOfGroupCache.put(id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        Set<String> groupIds = directParentGroupsOfGroupCache.get(id);

        if (groupIds != null)
            return Lists.newArrayList(groupIds);

        List<String> result = directoryBackend.getDirectParentGroupIdsOfGroup(id);

        if (entryCacheEnabled)
            directParentGroupsOfGroupCache.put(id, new HashSet<>(result));

        return result;
    }

    @Override
    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> groupIds = new ArrayList<>();
        GroupEntity group = getGroup(id);

        groupIds.add(group.getId());
        resolveGroupsDownwards(group.getId(), groupIds);
        groupIds.remove(group.getId());

        return groupIds;
    }

    @Override
    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> groupIds = new ArrayList<>();
        GroupEntity group = getGroup(id);

        groupIds.add(group.getId());
        resolveGroupsUpwards(group.getId(), groupIds);
        groupIds.remove(group.getId());

        return groupIds;
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

    private <T> void saveGroupEntities(Map<T, Set<String>> cache, T id, Set<GroupEntity> groups) {

        groups.forEach(x -> groupIdToEntityCache.put(x.getId(), x));
        cache.put(id, groups.stream().map(Entity::getId).collect(Collectors.toSet()));
    }

    private <T> void saveUserEntities(Map<T, Set<String>> cache, T id, Set<UserEntity> users) {

        users.forEach(x -> userIdToEntityCache.put(x.getId(), x));
        cache.put(id, users.stream().map(Entity::getId).collect(Collectors.toSet()));
    }

    private <K, V> Map<K, V> createCache() {

        return Collections.synchronizedMap(new LruCacheMap<>(entryCacheMaxSize, entryCacheMaxAge));
    }

    private void resolveGroupsDownwards(String id, List<String> acc)
            throws EntityNotFoundException {

        List<String> result = getDirectChildGroupIdsOfGroup(id);

        result.removeAll(acc);
        acc.addAll(result);

        for (String x : result)
            resolveGroupsDownwards(x, acc);
    }

    private void resolveGroupsUpwards(String id, List<String> acc)
            throws EntityNotFoundException {

        List<String> result = getDirectParentGroupIdsOfGroup(id);

        result.removeAll(acc);
        acc.addAll(result);

        for (String x : result)
            resolveGroupsUpwards(x, acc);
    }
}
