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
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.backend.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.naming.OperationNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The JSON directory backend API for test scenarios.
 */
public class JsonDirectoryBackend
        implements NestedDirectoryBackend {

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);
    private final List<Group> groupList = new ArrayList<>();
    private final List<User> userList = new ArrayList<>();
    private final File dbFile;

    /**
     * Instantiates a new JSON directory backend.
     *
     * @param config the config instance of the server
     */
    public JsonDirectoryBackend(ServerConfiguration config) {

        try {

            String urlString = config.getBackendProperties().getProperty("db-uri");

            if (urlString == null)
                throw new IllegalArgumentException("Missing value for db-uri");

            URL url;

            if (urlString.startsWith("classpath:")) {

                url = getClass().getClassLoader().getResource(urlString.substring(10));

                if (url == null)
                    throw new IllegalArgumentException("Cannot get resource from URL: " + urlString);

            } else {

                url = new URL(urlString);
            }

            dbFile = new File(url.getFile());

        } catch (MalformedURLException e) {

            throw new IllegalArgumentException(e);
        }
    }

    public String getId() {

        return "json";
    }

    public void startup() {

        try {

            Gson gson = new Gson();

            JsonObject jsonObject =
                    gson.fromJson(
                            new InputStreamReader(new FileInputStream(dbFile), StandardCharsets.UTF_8),
                            JsonObject.class);

            JsonArray groupNode = jsonObject.getAsJsonArray("groups");
            JsonArray userNode = jsonObject.getAsJsonArray("users");

            for (JsonElement element : userNode) {

                userList.add(new User(
                        element.getAsJsonObject().get("id").getAsString(),
                        element.getAsJsonObject().get("last_name").getAsString(),
                        element.getAsJsonObject().get("first_name").getAsString(),
                        element.getAsJsonObject().get("display_name").getAsString(),
                        element.getAsJsonObject().get("email").getAsString(),
                        element.getAsJsonObject().get("password").getAsString(),
                        element.getAsJsonObject().get("active").getAsBoolean()
                ));
            }

            for (JsonElement x : groupNode) {

                groupList.add(new Group(
                        x.getAsJsonObject().get("id").getAsString(),
                        x.getAsJsonObject().get("description").getAsString()
                ));
            }

            for (JsonElement x : groupNode) {

                String groupId = x.getAsJsonObject().get("id").getAsString();
                JsonArray groupMemberNode = x.getAsJsonObject().getAsJsonArray("group_members");
                JsonArray userMemberNode = x.getAsJsonObject().getAsJsonArray("user_members");

                Group group = groupList.stream().filter(z -> z.getId().equals(groupId)).findAny()
                        .orElseThrow(() -> new IllegalStateException("Unknown error."));

                for (JsonElement y : groupMemberNode)
                    group.addGroup(groupList.stream().filter(z -> z.getId().equals(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find group member with id " + y.getAsString()
                            )));

                for (JsonElement y : userMemberNode)
                    group.addUser(userList.stream().filter(z -> z.getId().equals(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find user member with id " + y.getAsString()
                            )));
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }

    public void shutdown() {

        groupList.clear();
        userList.clear();
    }

    public boolean isKnownGroup(String id) {

        try {

            findGroupById(id);
            return true;

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    public boolean isKnownUser(String id) {

        try {

            findUserById(id);
            return true;

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getGroup; id={}", id);

        return findGroupById(id);
    }

    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        logger.info("Call: getUser; id={}", id);

        return findUserById(id);
    }

    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        logger.info("Call: getAuthenticatedUser; id={}", id);

        User user = findUserById(id);

        if (!user.getPassword().equals(password))
            throw new SecurityProblemException("Could not authenticate user with id " + id);

        return user;
    }

    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        logger.info("Call: getGroups");

        return groupList.stream()
                .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher, int startIndex, int maxResults) {

        return getGroups(filterNode, filterMatcher).subList(startIndex, startIndex + maxResults);
    }

    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        logger.info("Call: getUsers");

        return userList.stream()
                .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher, int startIndex, int maxResults) {

        return getUsers(filterNode, filterMatcher).subList(startIndex, startIndex + maxResults);
    }

    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectUsersOfGroup; id={}", id);

        Group group = findGroupById(id);

        return new ArrayList<>(group.userMembers);
    }

    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectGroupsOfUser; id={}", id);

        User user = findUserById(id);

        return groupList.stream()
                .filter(x -> x.getUserMembers().contains(user))
                .collect(Collectors.toList());
    }

    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveUsersOfGroup; id={}", id);

        List<UserEntity> users = getDirectUsersOfGroup(id);

        for (GroupEntity y : getTransitiveChildGroupsOfGroup(id))
            for (UserEntity x : getDirectUsersOfGroup(y.getId()))
                if (!users.contains(x))
                    users.add(x);

        return users;
    }

    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveGroupsOfUser; id={}", id);

        List<GroupEntity> groups = getDirectGroupsOfUser(id);

        for (GroupEntity y : new ArrayList<>(groups))
            for (GroupEntity x : getTransitiveParentGroupsOfGroup(y.getId()))
                if (!groups.contains(x))
                    groups.add(x);

        return groups;
    }

    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectChildGroupsOfGroup; id={}", id);

        Group group = findGroupById(id);

        return new ArrayList<>(group.groupMembers);
    }

    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectParentGroupsOfGroup; id={}", id);

        Group group = findGroupById(id);

        return groupList.stream()
                .filter(x -> x.getGroupMembers().contains(group))
                .collect(Collectors.toList());
    }

    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveChildGroupsOfGroup; id={}", id);

        List<Group> groups = new ArrayList<>();
        Group group = findGroupById(id);

        groups.add(group);
        resolveGroupsDownwards(group, groups);
        groups.remove(group);

        return new ArrayList<>(groups);
    }

    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveParentGroupsOfGroup; id={}", id);

        List<Group> groups = new ArrayList<>();
        Group group = findGroupById(id);

        groups.add(group);
        resolveGroupsUpwards(group, groups);
        groups.remove(group);

        return new ArrayList<>(groups);
    }

    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getDirectUsersOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        return getDirectGroupsOfUser(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getTransitiveUsersOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        return getTransitiveGroupsOfUser(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getDirectChildGroupsOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getDirectParentGroupsOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getTransitiveChildGroupsOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        return getTransitiveParentGroupsOfGroup(id).stream()
                .map(Entity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<MembershipEntity> getMemberships() {

        throw new RuntimeException(new OperationNotSupportedException(
                "The method getMemberships is not supported by JSON directory backend."));
    }

    private Group findGroupById(String id)
            throws EntityNotFoundException {

        return groupList.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("Cannot find group with id " + id));
    }

    private User findUserById(String id)
            throws EntityNotFoundException {

        return userList.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("Cannot find user with id " + id));
    }

    private void resolveGroupsDownwards(Group group, List<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntityNotFoundException {

        List<Group> result = group.getGroupMembers();

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsDownwards(x, acc);
    }

    private void resolveGroupsUpwards(Group group, List<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntityNotFoundException {

        List<Group> result =
                groupList.stream()
                        .filter(x -> x.getGroupMembers().contains(group))
                        .collect(Collectors.toList());

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsUpwards(x, acc);
    }

    private static class Group
            extends GroupEntity {

        private final List<Group> groupMembers = new ArrayList<>();
        private final List<User> userMembers = new ArrayList<>();

        /**
         * Instantiates a new Group.
         *
         * @param id          the id
         * @param description the description
         */
        public Group(String id, String description) {

            super(id, description);
        }

        /**
         * Add group.
         *
         * @param group the group
         */
        public void addGroup(Group group) {

            groupMembers.add(group);
        }

        /**
         * Add user.
         *
         * @param user the user
         */
        public void addUser(User user) {

            userMembers.add(user);
        }

        /**
         * Gets group members.
         *
         * @return the group members
         */
        public List<Group> getGroupMembers() {

            return new ArrayList<>(groupMembers);
        }

        /**
         * Gets user members.
         *
         * @return the user members
         */
        public List<User> getUserMembers() {

            return new ArrayList<>(userMembers);
        }
    }

    private static class User
            extends UserEntity {

        private final String password;

        /**
         * Instantiates a new User.
         *
         * @param id          the id
         * @param firstName   the first name
         * @param lastName    the last name
         * @param displayName the display name
         * @param email       the email
         * @param password    the password
         * @param active      the active flag
         */
        public User(String id, String lastName, String firstName, String displayName, String email, String password,
                    boolean active) {

            super(id, lastName, firstName, displayName, email, active);
            this.password = password;
        }

        /**
         * Gets password.
         *
         * @return the password
         */
        public String getPassword() {

            return password;
        }
    }
}
