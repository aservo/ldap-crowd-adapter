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

package de.aservo.ldap.adapter.backend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.aservo.ldap.adapter.ServerConfiguration;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import de.aservo.ldap.adapter.api.directory.exception.DirectoryAccessFailureException;
import de.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import de.aservo.ldap.adapter.api.directory.exception.SecurityProblemException;
import de.aservo.ldap.adapter.api.entity.EntityType;
import de.aservo.ldap.adapter.api.entity.GroupEntity;
import de.aservo.ldap.adapter.api.entity.MembershipEntity;
import de.aservo.ldap.adapter.api.entity.UserEntity;
import de.aservo.ldap.adapter.api.query.QueryExpression;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The JSON directory backend API for test scenarios.
 */
public class JsonDirectoryBackend
        implements NestedDirectoryBackend {

    private final Logger logger = LoggerFactory.getLogger(JsonDirectoryBackend.class);
    private final Set<Group> groupSet = new HashSet<>();
    private final Set<User> userSet = new HashSet<>();
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

                userSet.add(new User(
                        element.getAsJsonObject().get("username").getAsString(),
                        element.getAsJsonObject().get("last_name").getAsString(),
                        element.getAsJsonObject().get("first_name").getAsString(),
                        element.getAsJsonObject().get("display_name").getAsString(),
                        element.getAsJsonObject().get("email").getAsString(),
                        element.getAsJsonObject().get("password").getAsString(),
                        element.getAsJsonObject().get("active").getAsBoolean()
                ));
            }

            for (JsonElement x : groupNode) {

                groupSet.add(new Group(
                        x.getAsJsonObject().get("name").getAsString(),
                        x.getAsJsonObject().get("description").getAsString()
                ));
            }

            for (JsonElement x : groupNode) {

                String groupId = x.getAsJsonObject().get("name").getAsString();
                JsonArray groupMemberNode = x.getAsJsonObject().getAsJsonArray("group_members");
                JsonArray userMemberNode = x.getAsJsonObject().getAsJsonArray("user_members");

                Group group = groupSet.stream().filter(z -> z.getId().equalsIgnoreCase(groupId)).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown error."));

                for (JsonElement y : groupMemberNode)
                    group.addGroup(groupSet.stream().filter(z -> z.getId().equalsIgnoreCase(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find group member with id " + y.getAsString()
                            )));

                for (JsonElement y : userMemberNode)
                    group.addUser(userSet.stream().filter(z -> z.getId().equalsIgnoreCase(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find user member with id " + y.getAsString()
                            )));
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }

    public void shutdown() {

        groupSet.clear();
        userSet.clear();
    }

    public MappableCursor<Row> runQueryExpression(SchemaManager schemaManager, QueryExpression expression,
                                                  EntityType entityType) {

        throw new UnsupportedOperationException("Query generation not supported for JSON directory backend.");
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

    public Set<GroupEntity> getAllGroups() {

        logger.info("Call: getGroups");

        return new HashSet<>(groupSet);
    }

    @Override
    public Set<GroupEntity> getAllGroups(int startIndex, int maxResults) {

        return new HashSet<>(new ArrayList<>(groupSet).subList(startIndex, startIndex + maxResults));
    }

    public Set<UserEntity> getAllUsers() {

        logger.info("Call: getUsers");

        return new HashSet<>(userSet);
    }

    @Override
    public Set<UserEntity> getAllUsers(int startIndex, int maxResults) {

        return new HashSet<>(new ArrayList<>(userSet).subList(startIndex, startIndex + maxResults));
    }

    public Set<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectUsersOfGroup; id={}", id);

        Group group = findGroupById(id);

        return group.getUserMembers().stream()
                .map(x -> (UserEntity) x)
                .collect(Collectors.toSet());
    }

    public Set<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectGroupsOfUser; id={}", id);

        User user = findUserById(id);

        return groupSet.stream()
                .filter(x -> x.getUserMembers().contains(user))
                .collect(Collectors.toSet());
    }

    public Set<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveUsersOfGroup; id={}", id);

        Set<UserEntity> users = getDirectUsersOfGroup(id);

        for (GroupEntity group : getTransitiveChildGroupsOfGroup(id))
            users.addAll(getDirectUsersOfGroup(group.getId()));

        return users;
    }

    public Set<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveGroupsOfUser; id={}", id);

        Set<GroupEntity> groups = getDirectGroupsOfUser(id);

        for (GroupEntity group : new ArrayList<>(groups))
            groups.addAll(getTransitiveParentGroupsOfGroup(group.getId()));

        return groups;
    }

    public Set<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectChildGroupsOfGroup; id={}", id);

        Group group = findGroupById(id);

        return group.getGroupMembers().stream()
                .filter(x -> !x.equals(group))
                .collect(Collectors.toSet());
    }

    public Set<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getDirectParentGroupsOfGroup; id={}", id);

        Group group = findGroupById(id);

        return groupSet.stream()
                .filter(x -> x.getGroupMembers().contains(group))
                .filter(x -> !x.equals(group))
                .collect(Collectors.toSet());
    }

    public Set<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveChildGroupsOfGroup; id={}", id);

        Set<Group> groups = new HashSet<>();
        Group group = findGroupById(id);

        groups.add(group);
        resolveGroupsDownwards(group, groups);
        groups.remove(group);

        return new HashSet<>(groups);
    }

    public Set<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Call: getTransitiveParentGroupsOfGroup; id={}", id);

        Set<Group> groups = new HashSet<>();
        Group group = findGroupById(id);

        groups.add(group);
        resolveGroupsUpwards(group, groups);
        groups.remove(group);

        return new HashSet<>(groups);
    }

    public MappableCursor<MembershipEntity> getMemberships() {

        logger.info("Backend call: getMemberships");

        return MappableCursor.fromIterable(groupSet).map(group -> {

            return new MembershipEntity(group.getName(),
                    getDirectChildGroupsOfGroup(group.getId()).stream()
                            .map(GroupEntity::getName)
                            .collect(Collectors.toSet()),
                    getDirectUsersOfGroup(group.getId()).stream()
                            .map(UserEntity::getUsername)
                            .collect(Collectors.toSet()));
        });
    }

    private Group findGroupById(String id)
            throws EntityNotFoundException {

        return groupSet.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("Cannot find group with id " + id));
    }

    private User findUserById(String id)
            throws EntityNotFoundException {

        return userSet.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("Cannot find user with id " + id));
    }

    private void resolveGroupsDownwards(Group group, Set<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntityNotFoundException {

        Set<Group> result = group.getGroupMembers();

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsDownwards(x, acc);
    }

    private void resolveGroupsUpwards(Group group, Set<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntityNotFoundException {

        Set<Group> result =
                groupSet.stream()
                        .filter(x -> x.getGroupMembers().contains(group))
                        .collect(Collectors.toSet());

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsUpwards(x, acc);
    }

    private static class Group
            extends GroupEntity {

        private final Set<Group> groupMembers = new HashSet<>();
        private final Set<User> userMembers = new HashSet<>();

        /**
         * Instantiates a new Group.
         *
         * @param name        the id
         * @param description the description
         */
        public Group(String name, String description) {

            super(name, description);
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
        public Set<Group> getGroupMembers() {

            return new HashSet<>(groupMembers);
        }

        /**
         * Gets user members.
         *
         * @return the user members
         */
        public Set<User> getUserMembers() {

            return new HashSet<>(userMembers);
        }
    }

    private static class User
            extends UserEntity {

        private final String password;

        /**
         * Instantiates a new User.
         *
         * @param username    the username
         * @param firstName   the first name
         * @param lastName    the last name
         * @param displayName the display name
         * @param email       the email
         * @param password    the password
         * @param active      the active flag
         */
        public User(String username, String lastName, String firstName, String displayName, String email,
                    String password, boolean active) {

            super(username, lastName, firstName, displayName, email, active);
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
