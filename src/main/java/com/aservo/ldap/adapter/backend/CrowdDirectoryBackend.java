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

import com.aservo.ldap.adapter.ServerConfiguration;
import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.cursor.MappableCursor;
import com.aservo.ldap.adapter.api.database.Row;
import com.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import com.aservo.ldap.adapter.api.directory.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.api.directory.exception.SecurityProblemException;
import com.aservo.ldap.adapter.api.entity.EntityType;
import com.aservo.ldap.adapter.api.entity.GroupEntity;
import com.aservo.ldap.adapter.api.entity.MembershipEntity;
import com.aservo.ldap.adapter.api.entity.UserEntity;
import com.aservo.ldap.adapter.api.query.AndLogicExpression;
import com.aservo.ldap.adapter.api.query.EqualOperator;
import com.aservo.ldap.adapter.api.query.OrLogicExpression;
import com.aservo.ldap.adapter.api.query.QueryExpression;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.*;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Crowd client API directory backend.
 */
public class CrowdDirectoryBackend
        implements NestedDirectoryBackend {

    /**
     * The constant CONFIG_READINESS_CHECK.
     */
    public static final String CONFIG_READINESS_CHECK = "readiness-check";

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);
    private final CrowdClient crowdClient;
    private final boolean useReadinessCheck;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param config the config instance of the server
     */
    public CrowdDirectoryBackend(ServerConfiguration config) {

        Properties properties = config.getBackendProperties();

        useReadinessCheck = Boolean.parseBoolean(properties.getProperty(CONFIG_READINESS_CHECK, "true"));

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

    public MappableCursor<Row> runQueryExpression(SchemaManager schemaManager, QueryExpression expression,
                                                  EntityType entityType) {

        throw new UnsupportedOperationException("Query generation not supported for Crowd directory backend.");
    }

    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getGroup; ID={}", id);

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

        logger.info("Backend call: getUser; ID={}", id);

        try {

            UserEntity entity = createUserEntity(crowdClient.getUser(id));

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

        logger.info("Backend call: getAuthenticatedUser; ID={}", id);

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

    public Set<GroupEntity> getAllGroups() {

        return getAllGroups(0, Integer.MAX_VALUE);
    }

    public Set<GroupEntity> getAllGroups(int startIndex, int maxResults) {

        logger.info("Backend call: getGroups({}, {})", startIndex, maxResults);

        try {

            return crowdClient.searchGroups(NullRestrictionImpl.INSTANCE, startIndex, maxResults).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<UserEntity> getAllUsers() {

        return getAllUsers(0, Integer.MAX_VALUE);
    }

    public Set<UserEntity> getAllUsers(int startIndex, int maxResults) {

        logger.info("Backend call: getUsers({}, {})", startIndex, maxResults);

        try {

            return crowdClient.searchUsers(NullRestrictionImpl.INSTANCE, startIndex, maxResults).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toSet());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectUsersOfGroup; ID={}", id);

        try {

            return crowdClient.getUsersOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectGroupsOfUser; ID={}", id);

        try {

            return crowdClient.getGroupsForUser(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveUsersOfGroup; ID={}", id);

        try {

            return crowdClient.getNestedUsersOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveGroupsOfUser; ID={}", id);

        try {

            return crowdClient.getGroupsForNestedUser(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectChildGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getChildGroupsOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectParentGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getParentGroupsForGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveChildGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getNestedChildGroupsOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Set<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveParentGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getParentGroupsForNestedGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toSet());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public MappableCursor<MembershipEntity> getMemberships() {

        logger.info("Backend call: getMemberships");

        try {

            return MappableCursor.fromIterable(crowdClient.getMemberships()).map(membership -> {

                return new MembershipEntity(membership.getGroupName(),
                        membership.getChildGroupNames(),
                        membership.getUserNames());
            });

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

    private SearchRestriction createGroupSearchRestriction(QueryExpression expression) {

        if (expression instanceof AndLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.AND,
                    ((AndLogicExpression) expression).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (expression instanceof OrLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.OR,
                    ((OrLogicExpression) expression).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (expression instanceof EqualOperator) {

            switch (LdapUtils.normalizeAttribute(((EqualOperator) expression).getAttribute())) {

                case SchemaConstants.CN_AT_OID:

                    return new TermRestriction<>(
                            GroupTermKeys.NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) expression).getValue()
                    );

                case SchemaConstants.DESCRIPTION_AT_OID:

                    return new TermRestriction<>(
                            GroupTermKeys.DESCRIPTION,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) expression).getValue()
                    );

                default:
                    break;
            }
        }

        return NullRestrictionImpl.INSTANCE;
    }
}
