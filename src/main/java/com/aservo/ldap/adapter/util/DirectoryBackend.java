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
            throws SecurityProblemException, DirectoryAccessFailureException;

    void shutdown()
            throws SecurityProblemException, DirectoryAccessFailureException;

    Map<String, String> getGroupInfo(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    Map<String, String> getUserInfo(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    List<String> getGroups()
            throws SecurityProblemException, DirectoryAccessFailureException;

    List<String> getUsers()
            throws SecurityProblemException, DirectoryAccessFailureException;

    List<String> getGroupsByAttribute(String attribute, String value)
            throws SecurityProblemException, DirectoryAccessFailureException;

    List<String> getUsersByAttribute(String attribute, String value)
            throws SecurityProblemException, DirectoryAccessFailureException;

    List<String> getUsersOfGroup(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    List<String> getGroupsOfUser(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    List<String> getChildGroups(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;

    List<String> getParentGroups(String id)
            throws EntryNotFoundException, SecurityProblemException, DirectoryAccessFailureException;
}
