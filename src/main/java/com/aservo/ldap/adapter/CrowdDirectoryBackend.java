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

package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import com.aservo.ldap.adapter.util.Utils;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.*;


public class CrowdDirectoryBackend
        implements DirectoryBackend {

    private final String id;
    private final String rootDnString;
    private final String groupDnString;
    private final String userDnString;

    private final CrowdClient crowdClient;

    public CrowdDirectoryBackend(Properties properties) {

        id = "Crowd";
        rootDnString = "dc=" + getId().toLowerCase();
        groupDnString = "ou=" + Utils.OU_GROUPS + ",dc=" + getId().toLowerCase();
        userDnString = "ou=" + Utils.OU_USERS + ",dc=" + getId().toLowerCase();

        ClientProperties props = ClientPropertiesImpl.newInstanceFromProperties(properties);
        crowdClient = new RestCrowdClientFactory().newInstance(props);
    }

    public String getId() {

        return id;
    }

    public String getRootDnString() {

        return rootDnString;
    }

    public String getGroupDnString() {

        return groupDnString;
    }

    public String getUserDnString() {

        return userDnString;
    }

    public void startup()
            throws SecurityProblemException, DirectoryAccessFailureException {

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
            throws SecurityProblemException, DirectoryAccessFailureException {

        crowdClient.shutdown();
    }

    public Map<String, String> getGroupInfo(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

        try {

            Group group = crowdClient.getGroup(id);

            Map<String, String> map = new HashMap<>();

            map.put(GROUP_ID, group.getName());
            map.put(GROUP_DESCRIPTION, group.getDescription());

            return map;

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
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

        try {

            User user = crowdClient.getUser(id);

            Map<String, String> map = new HashMap<>();

            map.put(USER_ID, user.getName());
            map.put(USER_FIRST_NAME, user.getFirstName());
            map.put(USER_LAST_NAME, user.getLastName());
            map.put(USER_DISPLAY_NAME, user.getDisplayName());
            map.put(USER_EMAIL_ADDRESS, user.getEmailAddress());

            return map;

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
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

        try {

            User user = crowdClient.authenticateUser(id, password);

            Map<String, String> map = new HashMap<>();

            map.put(USER_ID, user.getName());
            map.put(USER_FIRST_NAME, user.getFirstName());
            map.put(USER_LAST_NAME, user.getLastName());
            map.put(USER_DISPLAY_NAME, user.getDisplayName());
            map.put(USER_EMAIL_ADDRESS, user.getEmailAddress());

            return map;

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

    public List<String> getGroups()
            throws SecurityProblemException, DirectoryAccessFailureException {

        try {

            return crowdClient.searchGroupNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getUsers()
            throws SecurityProblemException, DirectoryAccessFailureException {

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
            throws SecurityProblemException, DirectoryAccessFailureException {

        try {

            if (attribute.equals(GROUP_ID)) {

                SearchRestriction restriction =
                        new TermRestriction<>(GroupTermKeys.NAME, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchGroupNames(restriction, 0, Integer.MAX_VALUE);

            } else if (attribute.equals(GROUP_DESCRIPTION)) {

                SearchRestriction restriction =
                        new TermRestriction<>(GroupTermKeys.DESCRIPTION, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchGroupNames(restriction, 0, Integer.MAX_VALUE);

            } else {

                return Collections.emptyList();
            }

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getUsersByAttribute(String attribute, String value)
            throws SecurityProblemException, DirectoryAccessFailureException {

        try {

            if (attribute.equals(USER_ID)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

            } else if (attribute.equals(USER_FIRST_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.FIRST_NAME, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

            } else if (attribute.equals(USER_LAST_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.LAST_NAME, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

            } else if (attribute.equals(USER_DISPLAY_NAME)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.DISPLAY_NAME, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

            } else if (attribute.equals(USER_EMAIL_ADDRESS)) {

                SearchRestriction restriction =
                        new TermRestriction<>(UserTermKeys.EMAIL, MatchMode.EXACTLY_MATCHES, value);

                return crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

            } else {

                return Collections.emptyList();
            }

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getUsersOfGroup(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

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

    public List<String> getGroupsOfUser(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

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

    public List<String> getChildGroups(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

        try {

            return crowdClient.getNamesOfNestedChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getParentGroups(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException {

        try {

            return crowdClient.getNamesOfParentGroupsForNestedGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntryNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }
}
