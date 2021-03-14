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
import com.aservo.ldap.adapter.adapter.LdapUtils;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.AndLogicExpression;
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.adapter.query.OrLogicExpression;
import com.aservo.ldap.adapter.backend.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.*;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Crowd client API directory backend.
 */
public class CrowdDirectoryBackend
        implements DirectoryBackend {

    /**
     * The constant CONFIG_READINESS_CHECK.
     */
    public static final String CONFIG_READINESS_CHECK = "readiness-check";
    /**
     * The constant CONFIG_PASS_ACTIVE_USERS_ONLY.
     */
    public static final String CONFIG_PASS_ACTIVE_USERS_ONLY = "pass-active-users-only";

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);
    private final ServerConfiguration config;
    private final CrowdClient crowdClient;
    private final boolean useReadinessCheck;
    private final boolean activeUsersOnly;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param config the config instance of the server
     */
    public CrowdDirectoryBackend(ServerConfiguration config) {

        Properties properties = config.getBackendProperties();

        this.config = config;

        useReadinessCheck = Boolean.parseBoolean(properties.getProperty(CONFIG_READINESS_CHECK, "true"));
        activeUsersOnly = Boolean.parseBoolean(properties.getProperty(CONFIG_PASS_ACTIVE_USERS_ONLY, "false"));

        ClientProperties props = ClientPropertiesImpl.newInstanceFromProperties(properties);
        crowdClient = new RestCrowdClientFactory().newInstance(props);
    }

    public String getId() {

        return "crowd";
    }

    public void startup() {

        try {

            if (useReadinessCheck)
                crowdClient.testConnection();

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public void shutdown() {

        crowdClient.shutdown();
    }

    public boolean isKnownGroup(String id) {

        return false;
    }

    public boolean isKnownUser(String id) {

        return false;
    }

    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getGroup; id={}", id);

        try {

            return createGroupEntity(crowdClient.getGroup(id));

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getUser; id={}", id);

        try {

            UserEntity entity = createUserEntity(crowdClient.getUser(id));

            if (activeUsersOnly && !entity.isActive())
                throw new UserNotFoundException("Will not deliver an inactive user.");

            return entity;

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        logger.info("Backend call: getAuthenticatedUser; id={}", id);

        try {

            return createUserEntity(crowdClient.authenticateUser(id, password));

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (InactiveAccountException |
                ExpiredCredentialException |
                ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        logger.info("Backend call: getGroups");

        SearchRestriction restriction =
                removeNullRestrictions(createGroupSearchRestriction(LdapUtils.removeNotExpressions(filterNode)));

        try {

            return crowdClient.searchGroups(restriction, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                    .collect(Collectors.toList());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        logger.info("Backend call: getUsers");

        SearchRestriction restriction =
                removeNullRestrictions(createUserSearchRestriction(LdapUtils.removeNotExpressions(filterNode)));

        if (activeUsersOnly)
            restriction = new BooleanRestrictionImpl(BooleanRestriction.BooleanLogic.AND, restriction,
                    new TermRestriction<>(UserTermKeys.ACTIVE, MatchMode.EXACTLY_MATCHES, true));

        try {

            return crowdClient.searchUsers(restriction, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                    .collect(Collectors.toList());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectUsersOfGroup; id={}", id);

        try {

            return crowdClient.getUsersOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectGroupsOfUser; id={}", id);

        try {

            return crowdClient.getGroupsForUser(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveUsersOfGroup; id={}", id);

        List<UserEntity> users = getDirectUsersOfGroup(id);

        for (GroupEntity y : getTransitiveChildGroupsOfGroup(id))
            for (UserEntity x : getDirectUsersOfGroup(y.getId()))
                if (!users.contains(x))
                    users.add(x);

        return users;
    }

    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveGroupsOfUser; id={}", id);

        List<GroupEntity> groups = getDirectGroupsOfUser(id);

        for (GroupEntity y : new ArrayList<>(groups))
            for (GroupEntity x : getTransitiveParentGroupsOfGroup(y.getId()))
                if (!groups.contains(x))
                    groups.add(x);

        return groups;
    }

    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectChildGroupsOfGroup; id={}", id);

        try {

            return crowdClient.getChildGroupsOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectParentGroupsOfGroup; id={}", id);

        try {

            return crowdClient.getParentGroupsForGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveChildGroupsOfGroup; id={}", id);

        List<String> groupIds = getTransitiveChildGroupIdsOfGroup(id);

        if (groupIds.isEmpty())
            return Collections.emptyList();

        return getGroups(new OrLogicExpression(
                groupIds.stream()
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT, x))
                        .collect(Collectors.toList())
        ), Optional.empty());

    }

    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveParentGroupsOfGroup; id={}", id);

        List<String> groupIds = getTransitiveParentGroupIdsOfGroup(id);

        if (groupIds.isEmpty())
            return Collections.emptyList();

        return getGroups(new OrLogicExpression(
                groupIds.stream()
                        .map(x -> new EqualOperator(SchemaConstants.CN_AT, x))
                        .collect(Collectors.toList())
        ), Optional.empty());
    }

    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectUserIdsOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfUsersOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectGroupIdsOfUser; id={}", id);

        try {

            return crowdClient.getNamesOfGroupsForUser(id, 0, Integer.MAX_VALUE);

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveUserIdsOfGroup; id={}", id);

        List<String> userIds = getDirectUserIdsOfGroup(id);

        for (String y : getTransitiveChildGroupIdsOfGroup(id))
            for (String x : getDirectUserIdsOfGroup(y))
                if (!userIds.contains(x))
                    userIds.add(x);

        return userIds;
    }

    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveGroupIdsOfUser; id={}", id);

        List<String> groupIds = getDirectGroupIdsOfUser(id);

        for (String y : new ArrayList<>(groupIds))
            for (String x : getTransitiveParentGroupIdsOfGroup(y))
                if (!groupIds.contains(x))
                    groupIds.add(x);

        return groupIds;
    }

    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectChildGroupIdsOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectParentGroupIdsOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfParentGroupsForGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveChildGroupIdsOfGroup; id={}", id);

        List<String> groupIds = new ArrayList<>();
        GroupEntity group = getGroup(id);

        groupIds.add(group.getId());
        resolveGroupsDownwards(group.getId(), groupIds);
        groupIds.remove(group.getId());

        return groupIds;
    }

    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveParentGroupIdsOfGroup; id={}", id);

        List<String> groupIds = new ArrayList<>();
        GroupEntity group = getGroup(id);

        groupIds.add(group.getId());
        resolveGroupsUpwards(group.getId(), groupIds);
        groupIds.remove(group.getId());

        return groupIds;
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        try {

            return crowdClient.isGroupDirectGroupMember(groupId1, groupId2);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        try {

            return crowdClient.isUserDirectGroupMember(userId, groupId);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private GroupEntity createGroupEntity(Group group) {

        return new GroupEntity(
                group.getName(),
                group.getDescription()
        );
    }

    private UserEntity createUserEntity(User user) {

        return new UserEntity(
                user.getName(),
                user.getLastName(),
                user.getFirstName(),
                user.getDisplayName(),
                user.getEmailAddress(),
                user.isActive()
        );
    }

    private void resolveGroupsDownwards(String id, List<String> acc)
            throws EntityNotFoundException {

        try {

            List<String> result = crowdClient.getNamesOfChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

            result.removeAll(acc);
            acc.addAll(result);

            for (String x : result)
                resolveGroupsDownwards(x, acc);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private void resolveGroupsUpwards(String id, List<String> acc)
            throws EntityNotFoundException {

        try {

            List<String> result = crowdClient.getNamesOfParentGroupsForGroup(id, 0, Integer.MAX_VALUE);

            result.removeAll(acc);
            acc.addAll(result);

            for (String x : result)
                resolveGroupsUpwards(x, acc);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private SearchRestriction createGroupSearchRestriction(FilterNode filterNode) {

        if (filterNode instanceof AndLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.AND,
                    ((AndLogicExpression) filterNode).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof OrLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.OR,
                    ((OrLogicExpression) filterNode).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof EqualOperator) {

            switch (LdapUtils.normalizeAttribute(((EqualOperator) filterNode).getAttribute())) {

                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    return new TermRestriction<>(
                            GroupTermKeys.NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.DESCRIPTION_AT:
                case SchemaConstants.DESCRIPTION_AT_OID:

                    return new TermRestriction<>(
                            GroupTermKeys.DESCRIPTION,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                default:
                    break;
            }
        }

        return NullRestrictionImpl.INSTANCE;
    }

    private SearchRestriction createUserSearchRestriction(FilterNode filterNode) {

        if (filterNode instanceof AndLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.AND,
                    ((AndLogicExpression) filterNode).getChildren().stream()
                            .map(this::createUserSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof OrLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.OR,
                    ((OrLogicExpression) filterNode).getChildren().stream()
                            .map(this::createUserSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof EqualOperator) {

            switch (LdapUtils.normalizeAttribute(((EqualOperator) filterNode).getAttribute())) {

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    return new TermRestriction<>(
                            UserTermKeys.USERNAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.SN_AT:
                case SchemaConstants.SN_AT_OID:
                case SchemaConstants.SURNAME_AT:

                    return new TermRestriction<>(
                            UserTermKeys.LAST_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.GN_AT:
                case SchemaConstants.GN_AT_OID:
                case SchemaConstants.GIVENNAME_AT:

                    return new TermRestriction<>(
                            UserTermKeys.FIRST_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.DISPLAY_NAME_AT:
                case SchemaConstants.DISPLAY_NAME_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.DISPLAY_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.EMAIL,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                default:
                    break;
            }
        }

        return NullRestrictionImpl.INSTANCE;
    }

    private SearchRestriction removeNullRestrictions(SearchRestriction restriction) {

        if (restriction instanceof BooleanRestriction) {

            List<SearchRestriction> sr =
                    ((BooleanRestriction) restriction).getRestrictions().stream()
                            .map(this::removeNullRestrictions)
                            .filter(x -> !(x instanceof NullRestriction))
                            .collect(Collectors.toList());

            if (sr.size() == 0)
                return NullRestrictionImpl.INSTANCE;
            else if (sr.size() == 1)
                return removeNullRestrictions(sr.get(0));
            else
                return new BooleanRestrictionImpl(((BooleanRestriction) restriction).getBooleanLogic(), sr);
        }

        return restriction;
    }
}
