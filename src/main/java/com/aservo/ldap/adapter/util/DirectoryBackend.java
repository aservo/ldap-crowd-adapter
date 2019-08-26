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


public interface DirectoryBackend {

    String GROUP_ID = "GROUP_ID";
    String GROUP_DESCRIPTION = "GROUP_DESCRIPTION";

    String USER_ID = "USER_ID";
    String USER_FIRST_NAME = "USER_FIRST_NAME";
    String USER_LAST_NAME = "USER_LAST_NAME";
    String USER_DISPLAY_NAME = "USER_DISPLAY_NAME";
    String USER_EMAIL_ADDRESS = "USER_EMAIL_ADDRESS";

    String getId();

    String getRootDnString();

    String getGroupDnString();

    String getUserDnString();

    void startup()
            throws DirectoryAccessFailureException, SecurityProblemException;

    void shutdown()
            throws DirectoryAccessFailureException, SecurityProblemException;

    Map<String, String> getGroupInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    Map<String, String> getUserInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getAllGroups()
            throws DirectoryAccessFailureException, SecurityProblemException;

    List<String> getAllUsers()
            throws DirectoryAccessFailureException, SecurityProblemException;

    List<String> getGroupsByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException;

    List<String> getUsersByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException;

    List<String> getDirectUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getDirectGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getTransitiveUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getTransitiveGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getDirectChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getDirectParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getTransitiveChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    List<String> getTransitiveParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    boolean isGroupDirectGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    boolean isUserDirectGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    boolean isGroupTransitiveGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;

    boolean isUserTransitiveGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException;
}
