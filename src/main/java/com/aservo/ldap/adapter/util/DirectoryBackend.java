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

package com.aservo.ldap.adapter.util;

import com.aservo.ldap.adapter.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import java.util.List;
import java.util.Map;


/**
 * The interface for all directory backends.
 */
public interface DirectoryBackend {

    /**
     * The constant GROUP_ID.
     */
    String GROUP_ID = "GROUP_ID";
    /**
     * The constant GROUP_DESCRIPTION.
     */
    String GROUP_DESCRIPTION = "GROUP_DESCRIPTION";

    /**
     * The constant USER_ID.
     */
    String USER_ID = "USER_ID";
    /**
     * The constant USER_FIRST_NAME.
     */
    String USER_FIRST_NAME = "USER_FIRST_NAME";
    /**
     * The constant USER_LAST_NAME.
     */
    String USER_LAST_NAME = "USER_LAST_NAME";
    /**
     * The constant USER_DISPLAY_NAME.
     */
    String USER_DISPLAY_NAME = "USER_DISPLAY_NAME";
    /**
     * The constant USER_EMAIL_ADDRESS.
     */
    String USER_EMAIL_ADDRESS = "USER_EMAIL_ADDRESS";

    /**
     * Gets backend ID.
     *
     * @return the backend ID
     */
    String getId();

    /**
     * Startup method.
     *
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    void startup()
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Shutdown method.
     *
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    void shutdown()
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets group info.
     *
     * @param id the id
     * @return the group info
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    Map<String, String> getGroupInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets user info.
     *
     * @param id the id
     * @return the user info
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    Map<String, String> getUserInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets info from authenticated user.
     *
     * @param id       the id
     * @param password the password
     * @return the info from authenticated user
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets all groups.
     *
     * @return the all groups
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getAllGroups()
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets all users.
     *
     * @return the all users
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getAllUsers()
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets groups by attribute.
     *
     * @param attribute the attribute
     * @param value     the value
     * @return the groups by attribute
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getGroupsByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets users by attribute.
     *
     * @param attribute the attribute
     * @param value     the value
     * @return the users by attribute
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getUsersByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets groups by attributes.
     *
     * @param attributeMap the attribute map
     * @return the groups by attributes
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getGroupsByAttributes(Map<String, String> attributeMap)
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets users by attributes.
     *
     * @param attributeMap the attribute map
     * @return the users by attributes
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     */
    List<String> getUsersByAttributes(Map<String, String> attributeMap)
            throws DirectoryAccessFailureException, SecurityProblemException;

    /**
     * Gets direct users of group.
     *
     * @param id the id
     * @return the direct users of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getDirectUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets direct groups of user.
     *
     * @param id the id
     * @return the direct groups of user
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getDirectGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets transitive users of group.
     *
     * @param id the id
     * @return the transitive users of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getTransitiveUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets transitive groups of user.
     *
     * @param id the id
     * @return the transitive groups of user
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getTransitiveGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets direct child groups of group.
     *
     * @param id the id
     * @return the direct child groups of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getDirectChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets direct parent groups of group.
     *
     * @param id the id
     * @return the direct parent groups of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getDirectParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets transitive child groups of group.
     *
     * @param id the id
     * @return the transitive child groups of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getTransitiveChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Gets transitive parent groups of group.
     *
     * @param id the id
     * @return the transitive parent groups of group
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    List<String> getTransitiveParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Indicates whether the group is direct group member.
     *
     * @param groupId1 the group id 1
     * @param groupId2 the group id 2
     * @return the boolean
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    boolean isGroupDirectGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Indicates whether the user is direct group member.
     *
     * @param userId  the user id
     * @param groupId the group id
     * @return the boolean
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    boolean isUserDirectGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Indicates whether the group is transitive group member.
     *
     * @param groupId1 the group id 1
     * @param groupId2 the group id 2
     * @return the boolean
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    boolean isGroupTransitiveGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    /**
     * Indicates whether the user is transitive group member.
     *
     * @param userId  the user id
     * @param groupId the group id
     * @return the boolean
     * @throws DirectoryAccessFailureException the directory access failure exception
     * @throws SecurityProblemException        the security problem exception
     * @throws EntryNotFoundException          the entry not found exception
     */
    boolean isUserTransitiveGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;
}
