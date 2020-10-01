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

import com.aservo.ldap.adapter.backend.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.backend.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Crowd client API directory backend.
 */
public class CrowdDirectoryBackend
        implements DirectoryBackend {

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);

    private final String id;
    private final CrowdClient crowdClient;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param properties the properties
     */
    public CrowdDirectoryBackend(Properties properties) {

        id = "Crowd";

        ClientProperties props = ClientPropertiesImpl.newInstanceFromProperties(properties);
        crowdClient = new RestCrowdClientFactory().newInstance(props);
    }

    public String getId() {

        return id;
    }

    public void startup()
            throws DirectoryAccessFailureException, SecurityProblemException {

        try {

            crowdClient.testConnection();

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public void shutdown()
            throws DirectoryAccessFailureException, SecurityProblemException {

        crowdClient.shutdown();
    }

    public Map<String, String> getGroupInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getGroupInfo; id={}", id);

        try {

            return mapGroupInfo(crowdClient.getGroup(id));

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Map<String, String> getUserInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getUserInfo; id={}", id);

        try {

            return mapUserInfo(crowdClient.getUser(id));

        } catch (UserNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getInfoFromAuthenticatedUser; id={}", id);

        try {

            return mapUserInfo(crowdClient.authenticateUser(id, password));

        } catch (UserNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (InactiveAccountException |
                ExpiredCredentialException |
                ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private Map<String, String> mapGroupInfo(Group group) {

        Map<String, String> map = new HashMap<>();

        map.put(GROUP_ID, group.getName());
        map.put(GROUP_DESCRIPTION, group.getDescription());

        return map;
    }

    private Map<String, String> mapUserInfo(User user) {

        Map<String, String> map = new HashMap<>();

        map.put(USER_ID, user.getName());
        map.put(USER_FIRST_NAME, user.getFirstName());
        map.put(USER_LAST_NAME, user.getLastName());
        map.put(USER_DISPLAY_NAME, user.getDisplayName());
        map.put(USER_EMAIL_ADDRESS, user.getEmailAddress());

        return map;
    }

    public List<String> getAllGroups()
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getAllGroups");

        try {

            return crowdClient.searchGroupNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getAllUsers()
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getAllUsers");

        try {

            return crowdClient.searchUserNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getGroupsByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException {

        Map<String, String> map = new HashMap<>();

        map.put(attribute, value);

        return getGroupsByAttributes(map);
    }

    public List<String> getUsersByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException {

        Map<String, String> map = new HashMap<>();

        map.put(attribute, value);

        return getUsersByAttributes(map);
    }

    public List<String> getGroupsByAttributes(Map<String, String> attributeMap)
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getGroupsByAttributes");

        if (attributeMap.isEmpty())
            return getAllGroups();

        List<SearchRestriction> restrictions = new ArrayList<>(attributeMap.size());

        for (Map.Entry<String, String> entry : attributeMap.entrySet()) {

            if (entry.getKey().equals(GROUP_ID)) {

                SearchRestriction restriction =
                        new TermRestriction<>(GroupTermKeys.NAME, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else if (entry.getKey().equals(GROUP_DESCRIPTION)) {

                SearchRestriction restriction =
                        new TermRestriction<>(GroupTermKeys.DESCRIPTION, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else
                throw new IllegalArgumentException("Cannot process unknown group attribute " + entry.getKey());
        }

        try {

            SearchRestriction restriction =
                    new BooleanRestrictionImpl(BooleanRestriction.BooleanLogic.AND, restrictions);

            return crowdClient.searchGroupNames(restriction, 0, Integer.MAX_VALUE);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getUsersByAttributes(Map<String, String> attributeMap)
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getUsersByAttributes");

        if (attributeMap.isEmpty())
            return getAllUsers();

        List<SearchRestriction> restrictions = new ArrayList<>(attributeMap.size());

        for (Map.Entry<String, String> entry : attributeMap.entrySet()) {

            if (entry.getKey().equals(USER_ID)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else if (entry.getKey().equals(USER_FIRST_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.FIRST_NAME, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else if (entry.getKey().equals(USER_LAST_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.LAST_NAME, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else if (entry.getKey().equals(USER_DISPLAY_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.DISPLAY_NAME, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else if (entry.getKey().equals(USER_EMAIL_ADDRESS)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.EMAIL, MatchMode.EXACTLY_MATCHES, entry.getValue());

                restrictions.add(restriction);

            } else
                throw new IllegalArgumentException("Cannot process unknown user attribute " + entry.getKey());
        }

        try {

            SearchRestriction restriction =
                    new BooleanRestrictionImpl(BooleanRestriction.BooleanLogic.AND, restrictions);

            return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectUsersOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfUsersOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectGroupsOfUser; id={}", id);

        try {

            return crowdClient.getNamesOfGroupsForUser(id, 0, Integer.MAX_VALUE);

        } catch (UserNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveUsersOfGroup; id={}", id);

        List<String> userIds = getDirectUsersOfGroup(id);

        for (String y : getTransitiveChildGroupsOfGroup(id))
            for (String x : getDirectUsersOfGroup(y))
                if (!userIds.contains(x))
                    userIds.add(x);

        return userIds;
    }

    public List<String> getTransitiveGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveGroupsOfUser; id={}", id);

        List<String> groupIds = getDirectGroupsOfUser(id);

        for (String y : new ArrayList<>(groupIds))
            for (String x : getTransitiveParentGroupsOfGroup(y))
                if (!groupIds.contains(x))
                    groupIds.add(x);

        return groupIds;
    }

    public List<String> getDirectChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectChildGroupsOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectParentGroupsOfGroup; id={}", id);

        try {

            return crowdClient.getNamesOfParentGroupsForGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveChildGroupsOfGroup; id={}", id);

        List<String> groupIds = new ArrayList<>();

        groupIds.add(id);
        resolveGroupsDownwards(id, groupIds);
        groupIds.remove(id);

        return groupIds;
    }

    public List<String> getTransitiveParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveParentGroupsOfGroup; id={}", id);

        List<String> groupIds = new ArrayList<>();

        groupIds.add(id);
        resolveGroupsUpwards(id, groupIds);
        groupIds.remove(id);

        return groupIds;
    }

    private void resolveGroupsDownwards(String id, List<String> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        try {

            List<String> result = crowdClient.getNamesOfChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

            result.removeAll(acc);
            acc.addAll(result);

            for (String x : result)
                resolveGroupsDownwards(x, acc);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private void resolveGroupsUpwards(String id, List<String> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        try {

            List<String> result = crowdClient.getNamesOfParentGroupsForGroup(id, 0, Integer.MAX_VALUE);

            result.removeAll(acc);
            acc.addAll(result);

            for (String x : result)
                resolveGroupsUpwards(x, acc);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public boolean isGroupDirectGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isGroupDirectGroupMember");

        try {

            crowdClient.getGroup(groupId1);
            crowdClient.getGroup(groupId2);

            return crowdClient.isGroupDirectGroupMember(groupId1, groupId2);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public boolean isUserDirectGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isUserDirectGroupMember");

        try {

            crowdClient.getUser(userId);
            crowdClient.getGroup(groupId);

            return crowdClient.isUserDirectGroupMember(userId, groupId);

        } catch (GroupNotFoundException |
                UserNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isGroupTransitiveGroupMember");

        try {

            crowdClient.getGroup(groupId1);

            return getTransitiveParentGroupsOfGroup(groupId2).contains(groupId1);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public boolean isUserTransitiveGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isUserTransitiveGroupMember");

        try {

            crowdClient.getUser(userId);

            return getTransitiveUsersOfGroup(groupId).contains(userId);

        } catch (UserNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }
}
