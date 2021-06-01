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

import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;


public class CachedSessionDirectoryBackend
        extends CachedDirectoryBackend {

    private static final int directCacheHashMapInitialSize = 8192;
    private static final int transitiveCacheHashMapInitialSize = 8192;
    private static final int cacheEntryHashSetInitialSize = 16;

    private final Map<String, Set<String>> directUsersOfGroup = new HashMap<>(directCacheHashMapInitialSize);
    private final Map<String, Set<String>> directGroupsOfUser = new HashMap<>(directCacheHashMapInitialSize);
    private final Map<String, Set<String>> directChildGroupsOfGroup = new HashMap<>(directCacheHashMapInitialSize);
    private final Map<String, Set<String>> directParentGroupsOfGroup = new HashMap<>(directCacheHashMapInitialSize);
    private final Map<String, Set<String>> transitiveUsersOfGroup = new HashMap<>(transitiveCacheHashMapInitialSize);
    private final Map<String, Set<String>> transitiveGroupsOfUser = new HashMap<>(transitiveCacheHashMapInitialSize);
    private final Map<String, Set<String>> transitiveChildGroupsOfGroup = new HashMap<>(transitiveCacheHashMapInitialSize);
    private final Map<String, Set<String>> transitiveParentGroupsOfGroup = new HashMap<>(transitiveCacheHashMapInitialSize);

    private boolean initDirectUserRelationships = false;
    private boolean initDirectGroupRelationships = false;
    private boolean initTransitiveUserRelationships = false;
    private boolean initTransitiveGroupRelationships = false;

    private int usesToCacheDirectUserRelationships = 3;
    private int usesToCacheDirectGroupRelationships = 3;
    private int usesToCacheTransitiveUserRelationships = 1;
    private int usesToCacheTransitiveGroupRelationships = 1;

    /**
     * Instantiates a new directory backend.
     *
     * @param config           config the config instance of the server
     * @param directoryBackend the directory backend
     */
    public CachedSessionDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);
    }

    @Override
    public List<String> getDirectUserNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheDirectUserRelationships > 0) {
            usesToCacheDirectUserRelationships--;
            return directoryBackend.getDirectUserNamesOfGroup(id);
        }

        updateDirectUserRelationships();

        if (!directUsersOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(directUsersOfGroup.get(id));
    }

    @Override
    public List<String> getDirectGroupNamesOfUser(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheDirectUserRelationships > 0) {
            usesToCacheDirectUserRelationships--;
            return directoryBackend.getDirectGroupNamesOfUser(id);
        }

        updateDirectUserRelationships();

        if (!directGroupsOfUser.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(directGroupsOfUser.get(id));
    }

    @Override
    public List<String> getDirectChildGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheDirectGroupRelationships > 0) {
            usesToCacheDirectGroupRelationships--;
            return directoryBackend.getDirectChildGroupNamesOfGroup(id);
        }

        updateDirectGroupRelationships();

        if (!directChildGroupsOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(directChildGroupsOfGroup.get(id));
    }

    @Override
    public List<String> getDirectParentGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheDirectGroupRelationships > 0) {
            usesToCacheDirectGroupRelationships--;
            return directoryBackend.getDirectParentGroupNamesOfGroup(id);
        }

        updateDirectGroupRelationships();

        if (!directParentGroupsOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(directParentGroupsOfGroup.get(id));
    }

    @Override
    public List<String> getTransitiveUserNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheTransitiveUserRelationships > 0) {
            usesToCacheTransitiveUserRelationships--;
            return directoryBackend.getTransitiveUserNamesOfGroup(id);
        }

        updateTransitiveUserRelationships();

        if (!transitiveUsersOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(transitiveUsersOfGroup.get(id));
    }

    @Override
    public List<String> getTransitiveGroupNamesOfUser(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheTransitiveUserRelationships > 0) {
            usesToCacheTransitiveUserRelationships--;
            return directoryBackend.getTransitiveGroupNamesOfUser(id);
        }

        updateTransitiveUserRelationships();

        if (!transitiveGroupsOfUser.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(transitiveGroupsOfUser.get(id));
    }

    @Override
    public List<String> getTransitiveChildGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheTransitiveGroupRelationships > 0) {
            usesToCacheTransitiveGroupRelationships--;
            return directoryBackend.getTransitiveChildGroupNamesOfGroup(id);
        }

        updateTransitiveGroupRelationships();

        if (!transitiveChildGroupsOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(transitiveChildGroupsOfGroup.get(id));
    }

    @Override
    public List<String> getTransitiveParentGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        // below the configured number of queries, we use direct calls, then we start caching
        if (usesToCacheTransitiveGroupRelationships > 0) {
            usesToCacheTransitiveGroupRelationships--;
            return directoryBackend.getTransitiveParentGroupNamesOfGroup(id);
        }

        updateTransitiveGroupRelationships();

        if (!transitiveParentGroupsOfGroup.containsKey(id))
            return Collections.emptyList();

        return new ArrayList<>(transitiveParentGroupsOfGroup.get(id));
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        updateDirectGroupRelationships();

        if (!directChildGroupsOfGroup.containsKey(groupId2))
            return false;

        return directChildGroupsOfGroup.get(groupId2).contains(groupId1);
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        updateDirectUserRelationships();

        if (!directUsersOfGroup.containsKey(groupId))
            return false;

        return directUsersOfGroup.get(groupId).contains(userId);
    }

    @Override
    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2) {

        updateTransitiveGroupRelationships();

        if (!transitiveChildGroupsOfGroup.containsKey(groupId2))
            return false;

        return transitiveChildGroupsOfGroup.get(groupId2).contains(groupId1);
    }

    @Override
    public boolean isUserTransitiveGroupMember(String userId, String groupId) {

        updateTransitiveUserRelationships();

        if (!transitiveUsersOfGroup.containsKey(groupId))
            return false;

        return transitiveUsersOfGroup.get(groupId).contains(userId);
    }

    private void updateDirectUserRelationships() {

        if (!initDirectUserRelationships) {

            initDirectUserRelationships = true;

            usesToCacheDirectUserRelationships = 0;

            directoryBackend.getAllDirectUserRelationships()
                    .forEach(x -> updateCache(x, directUsersOfGroup, directGroupsOfUser));
        }
    }

    private void updateDirectGroupRelationships() {

        if (!initDirectGroupRelationships) {

            initDirectGroupRelationships = true;

            usesToCacheDirectGroupRelationships = 0;

            directoryBackend.getAllDirectGroupRelationships()
                    .forEach(x -> updateCache(x, directChildGroupsOfGroup, directParentGroupsOfGroup));
        }
    }

    private void updateTransitiveUserRelationships() {

        if (!initTransitiveUserRelationships) {

            initTransitiveUserRelationships = true;

            usesToCacheTransitiveUserRelationships = 0;

            directoryBackend.getAllTransitiveUserRelationships()
                    .forEach(x -> updateCache(x, transitiveUsersOfGroup, transitiveGroupsOfUser));
        }
    }

    private void updateTransitiveGroupRelationships() {

        if (!initTransitiveGroupRelationships) {

            initTransitiveGroupRelationships = true;

            usesToCacheTransitiveGroupRelationships = 0;

            directoryBackend.getAllTransitiveGroupRelationships()
                    .forEach(x -> updateCache(x, transitiveChildGroupsOfGroup, transitiveParentGroupsOfGroup));
        }
    }

    private void updateCache(
            Pair<String, String> pair,
            Map<String, Set<String>> cache,
            Map<String, Set<String>> cacheReverse) {

        if (!cache.containsKey(pair.getLeft()))
            cache.put(pair.getLeft(), new HashSet<>(cacheEntryHashSetInitialSize));

        if (!cacheReverse.containsKey(pair.getRight()))
            cacheReverse.put(pair.getRight(), new HashSet<>(cacheEntryHashSetInitialSize));

        cache.get(pair.getLeft()).add(pair.getRight());
        cacheReverse.get(pair.getRight()).add(pair.getLeft());
    }
}
