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
import com.aservo.ldap.adapter.adapter.entity.MembershipEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.adapter.query.OrLogicExpression;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.LruCacheMap;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A proxy for directory backends to cache entities via in-memory strategy.
 */
public class CachedInMemoryDirectoryBackend
        extends CachedDirectoryBackend {

    /**
     * The constant CONFIG_ENTRY_CACHE_ENABLED.
     */
    public static final String CONFIG_ENTRY_CACHE_ENABLED = "entry-cache.enabled";

    /**
     * The constant CONFIG_ENTRY_CACHE_MAX_SIZE.
     */
    public static final String CONFIG_ENTRY_CACHE_MAX_SIZE = "entry-cache.max-size";
    /**
     * The constant CONFIG_ENTRY_CACHE_MAX_AGE.
     */
    public static final String CONFIG_ENTRY_CACHE_MAX_AGE = "entry-cache.max-age";

    private final Logger logger = LoggerFactory.getLogger(CachedInMemoryDirectoryBackend.class);

    private final boolean entryCacheEnabled;

    private final Map<String, GroupEntity> groupIdToEntityCache;
    private final Map<String, UserEntity> userIdToEntityCache;
    private final Map<FilterNode, Set<String>> groupFilterToIdCache;
    private final Map<FilterNode, Set<String>> userFilterToIdCache;
    private final Map<String, Set<String>> directGroupsOfUserCache;
    private final Map<String, Set<String>> directUsersOfGroupCache;
    private final Map<String, Set<String>> directChildGroupsOfGroupCache;
    private final Map<String, Set<String>> directParentGroupsOfGroupCache;

    /**
     * Instantiates a new directory backend.
     *
     * @param config           config the config instance of the server
     * @param directoryBackend the directory backend
     */
    public CachedInMemoryDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);

        Properties properties = config.getBackendProperties();

        entryCacheEnabled = Boolean.parseBoolean(properties.getProperty(CONFIG_ENTRY_CACHE_ENABLED, "false"));
        int entryCacheMaxSize = Integer.parseInt(properties.getProperty(CONFIG_ENTRY_CACHE_MAX_SIZE, "300"));
        Duration entryCacheMaxAge = Duration.parse(properties.getProperty(CONFIG_ENTRY_CACHE_MAX_AGE, "PT1H"));

        if (entryCacheMaxSize <= 0)
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_SIZE);

        if (entryCacheMaxAge.isNegative() || entryCacheMaxAge.isZero())
            throw new IllegalArgumentException("Expect value greater than zero for " + CONFIG_ENTRY_CACHE_MAX_AGE);

        groupIdToEntityCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        userIdToEntityCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        groupFilterToIdCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        userFilterToIdCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        directGroupsOfUserCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        directUsersOfGroupCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        directChildGroupsOfGroupCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
        directParentGroupsOfGroupCache = createCache(entryCacheMaxSize, entryCacheMaxAge);
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
    public void upsertGroup(String id) {

        super.upsertGroup(id);

        if (!entryCacheEnabled)
            return;

        try {

            GroupEntity entity = directoryBackend.getGroup(id);

            groupIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : groupFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());

        } catch (EntityNotFoundException e) {

            logger.warn("The group entity no longer exists.", e);
        }
    }

    @Override
    public int upsertAllGroups(int startIndex, int maxResults) {

        super.upsertAllGroups(startIndex, maxResults);

        if (!entryCacheEnabled)
            return 0;

        List<GroupEntity> entities = directoryBackend.getAllGroups(startIndex, maxResults);

        entities.forEach(entity -> {

            groupIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : groupFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());
        });

        return entities.size();
    }

    @Override
    public int upsertAllGroups() {

        super.upsertAllGroups();

        if (!entryCacheEnabled)
            return 0;

        List<GroupEntity> entities = directoryBackend.getAllGroups();

        entities.forEach(entity -> {

            groupIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : groupFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());
        });

        return entities.size();
    }

    @Override
    public void upsertUser(String id) {

        super.upsertUser(id);

        if (!entryCacheEnabled)
            return;

        try {

            UserEntity entity = directoryBackend.getUser(id);

            userIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : userFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());

        } catch (EntityNotFoundException e) {

            logger.warn("The user entity no longer exists.", e);
        }
    }

    @Override
    public int upsertAllUsers(int startIndex, int maxResults) {

        super.upsertAllUsers(startIndex, maxResults);

        if (!entryCacheEnabled)
            return 0;

        List<UserEntity> entities = directoryBackend.getAllUsers(startIndex, maxResults);

        entities.forEach(entity -> {

            userIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : userFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());
        });

        return entities.size();
    }

    @Override
    public int upsertAllUsers() {

        super.upsertAllUsers();

        if (!entryCacheEnabled)
            return 0;

        List<UserEntity> entities = directoryBackend.getAllUsers();

        entities.forEach(entity -> {

            userIdToEntityCache.put(entity.getId(), entity);

            for (Map.Entry<FilterNode, Set<String>> entry : userFilterToIdCache.entrySet())
                entry.getValue().remove(entity.getId());
        });

        return entities.size();
    }

    @Override
    public void upsertMembership(MembershipEntity membership) {

        super.upsertMembership(membership);

        if (!entryCacheEnabled)
            return;

        Optional.ofNullable(directUsersOfGroupCache.get(membership.getParentGroupId()))
                .ifPresent(x -> x.addAll(membership.getMemberUserIds()));

        for (String userId : membership.getMemberUserIds())
            Optional.ofNullable(directGroupsOfUserCache.get(userId))
                    .ifPresent(x -> x.add(membership.getParentGroupId()));

        Optional.ofNullable(directChildGroupsOfGroupCache.get(membership.getParentGroupId()))
                .ifPresent(x -> x.addAll(membership.getMemberGroupIds()));

        for (String groupId : membership.getMemberGroupIds())
            Optional.ofNullable(directParentGroupsOfGroupCache.get(groupId))
                    .ifPresent(x -> x.add(membership.getParentGroupId()));
    }

    @Override
    public void dropGroup(String id) {

        super.dropGroup(id);

        if (!entryCacheEnabled)
            return;

        for (Map.Entry<FilterNode, Set<String>> entry : groupFilterToIdCache.entrySet())
            entry.getValue().remove(id);

        for (Map.Entry<String, Set<String>> entry : directGroupsOfUserCache.entrySet())
            entry.getValue().remove(id);

        for (Map.Entry<String, Set<String>> entry : directChildGroupsOfGroupCache.entrySet())
            entry.getValue().remove(id);

        for (Map.Entry<String, Set<String>> entry : directParentGroupsOfGroupCache.entrySet())
            entry.getValue().remove(id);

        directChildGroupsOfGroupCache.remove(id);
        directParentGroupsOfGroupCache.remove(id);
        directUsersOfGroupCache.remove(id);
        groupIdToEntityCache.remove(id);
    }

    @Override
    public void dropAllGroups() {

        super.dropAllGroups();

        if (!entryCacheEnabled)
            return;

        groupIdToEntityCache.clear();
        groupFilterToIdCache.clear();
        directChildGroupsOfGroupCache.clear();
        directParentGroupsOfGroupCache.clear();
    }

    @Override
    public void dropUser(String id) {

        super.dropUser(id);

        if (!entryCacheEnabled)
            return;

        for (Map.Entry<FilterNode, Set<String>> entry : userFilterToIdCache.entrySet())
            entry.getValue().remove(id);

        for (Map.Entry<String, Set<String>> entry : directUsersOfGroupCache.entrySet())
            entry.getValue().remove(id);

        directGroupsOfUserCache.remove(id);
        userIdToEntityCache.remove(id);
    }

    @Override
    public void dropAllUsers() {

        super.dropAllUsers();

        if (!entryCacheEnabled)
            return;

        userIdToEntityCache.clear();
        userFilterToIdCache.clear();
        directGroupsOfUserCache.clear();
        directUsersOfGroupCache.clear();
    }

    @Override
    public void dropMembership(MembershipEntity membership) {

        super.dropMembership(membership);

        if (!entryCacheEnabled)
            return;

        Optional.ofNullable(directUsersOfGroupCache.get(membership.getParentGroupId()))
                .ifPresent(x -> x.removeAll(membership.getMemberUserIds()));

        for (String userId : membership.getMemberUserIds())
            Optional.ofNullable(directGroupsOfUserCache.get(userId))
                    .ifPresent(x -> x.remove(membership.getParentGroupId()));

        Optional.ofNullable(directChildGroupsOfGroupCache.get(membership.getParentGroupId()))
                .ifPresent(x -> x.removeAll(membership.getMemberGroupIds()));

        for (String groupId : membership.getMemberGroupIds())
            Optional.ofNullable(directParentGroupsOfGroupCache.get(groupId))
                    .ifPresent(x -> x.remove(membership.getParentGroupId()));
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
    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> groupIds = getTransitiveChildGroupIdsOfGroup(id);

        if (groupIds.isEmpty())
            return Collections.emptyList();

        return getGroups(new OrLogicExpression(
                groupIds.stream()
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT_OID, x))
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
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT_OID, x))
                        .collect(Collectors.toList())
        ), Optional.empty());
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
    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> result = new ArrayList<>();
        GroupEntity group = getGroup(id);

        result.add(group.getId());
        resolveGroupsDownwards(group.getId(), result);
        result.remove(group.getId());

        return result;
    }

    @Override
    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        List<String> result = new ArrayList<>();
        GroupEntity group = getGroup(id);

        result.add(group.getId());
        resolveGroupsUpwards(group.getId(), result);
        result.remove(group.getId());

        return result;
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        try {

            return getDirectChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        try {

            return getDirectUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2) {

        try {

            return getTransitiveChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isUserTransitiveGroupMember(String userId, String groupId) {

        try {

            return getTransitiveUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
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

    @Override
    public Iterable<MembershipEntity> getMemberships() {

        Iterator<MembershipEntity> memberships = directoryBackend.getMemberships().iterator();

        return new Iterable<MembershipEntity>() {

            @NotNull
            @Override
            public Iterator<MembershipEntity> iterator() {

                return new Iterator<MembershipEntity>() {

                    @Override
                    public boolean hasNext() {

                        return memberships.hasNext();
                    }

                    @Override
                    public MembershipEntity next() {

                        MembershipEntity membership = memberships.next();

                        directChildGroupsOfGroupCache.put(membership.getParentGroupId(), membership.getMemberGroupIds());
                        directUsersOfGroupCache.put(membership.getParentGroupId(), membership.getMemberUserIds());

                        for (String groupId : membership.getMemberGroupIds())
                            Optional.ofNullable(directParentGroupsOfGroupCache.get(groupId))
                                    .ifPresent(x -> x.add(membership.getParentGroupId()));

                        for (String userId : membership.getMemberUserIds())
                            Optional.ofNullable(directGroupsOfUserCache.get(userId))
                                    .ifPresent(x -> x.add(membership.getParentGroupId()));

                        return membership;
                    }
                };
            }
        };
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

    private <K, V> Map<K, V> createCache(int entryCacheMaxSize, Duration entryCacheMaxAge) {

        return Collections.synchronizedMap(new LruCacheMap<>(entryCacheMaxSize, entryCacheMaxAge));
    }
}
