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

import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.OrLogicExpression;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;


public abstract class CachedDirectoryBackend
        extends ProxyDirectoryBackend {

    protected CachedDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);
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
