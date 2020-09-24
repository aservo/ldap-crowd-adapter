package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The JSON directory backend API for test scenarios.
 */
public class JsonDirectoryBackend
        implements DirectoryBackend {

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);

    private final String id;
    private final List<Group> groups = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    private final File dbFile;

    /**
     * Instantiates a new JSON directory backend.
     *
     * @param properties the properties
     */
    public JsonDirectoryBackend(Properties properties) {

        id = "JSON";

        try {

            String urlString = properties.getProperty("db-uri");

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

        return id;
    }

    public void startup()
            throws DirectoryAccessFailureException, SecurityProblemException {

        try {

            Gson gson = new Gson();

            JsonObject jsonObject =
                    gson.fromJson(
                            new InputStreamReader(new FileInputStream(dbFile), StandardCharsets.UTF_8),
                            JsonObject.class);

            JsonArray groupNode = jsonObject.getAsJsonArray("groups");
            JsonArray userNode = jsonObject.getAsJsonArray("users");

            for (JsonElement element : userNode) {

                users.add(new User(
                        element.getAsJsonObject().get("id").getAsString(),
                        element.getAsJsonObject().get("first_name").getAsString(),
                        element.getAsJsonObject().get("last_name").getAsString(),
                        element.getAsJsonObject().get("display_name").getAsString(),
                        element.getAsJsonObject().get("email").getAsString(),
                        element.getAsJsonObject().get("password").getAsString()
                ));
            }

            for (JsonElement x : groupNode) {

                groups.add(new Group(
                        x.getAsJsonObject().get("id").getAsString(),
                        x.getAsJsonObject().get("description").getAsString()
                ));
            }

            for (JsonElement x : groupNode) {

                String groupId = x.getAsJsonObject().get("id").getAsString();
                JsonArray groupMemberNode = x.getAsJsonObject().getAsJsonArray("group_members");
                JsonArray userMemberNode = x.getAsJsonObject().getAsJsonArray("user_members");

                Group group = groups.stream().filter(z -> z.getId().equals(groupId)).findAny()
                        .orElseThrow(() -> new IllegalStateException("Unknown error."));

                for (JsonElement y : groupMemberNode)
                    group.addGroup(groups.stream().filter(z -> z.getId().equals(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find group member with id " + y.getAsString()
                            )));

                for (JsonElement y : userMemberNode)
                    group.addUser(users.stream().filter(z -> z.getId().equals(y.getAsString())).findAny()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find user member with id " + y.getAsString()
                            )));
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }

    public void shutdown()
            throws DirectoryAccessFailureException, SecurityProblemException {

        groups.clear();
        users.clear();
    }

    private Group findGroupById(String id)
            throws EntryNotFoundException {

        return groups.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntryNotFoundException("Cannot find group with id " + id));
    }

    private User findUserById(String id)
            throws EntryNotFoundException {

        return users.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntryNotFoundException("Cannot find user with id " + id));
    }

    public Map<String, String> getGroupInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getGroupInfo; id={}", id);

        Group group = findGroupById(id);

        return mapGroupInfo(group);
    }

    public Map<String, String> getUserInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getUserInfo; id={}", id);

        User user = findUserById(id);

        return mapUserInfo(user);
    }

    public Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getInfoFromAuthenticatedUser; id={}", id);

        User user = findUserById(id);

        if (!user.getPassword().equals(password))
            throw new SecurityProblemException("Could not authenticate user with id " + id);

        return mapUserInfo(user);
    }

    private Map<String, String> mapGroupInfo(Group group) {

        Map<String, String> map = new HashMap<>();

        map.put(GROUP_ID, group.getId());
        map.put(GROUP_DESCRIPTION, group.getDescription());

        return map;
    }

    private Map<String, String> mapUserInfo(User user) {

        Map<String, String> map = new HashMap<>();

        map.put(USER_ID, user.getId());
        map.put(USER_FIRST_NAME, user.getFirstName());
        map.put(USER_LAST_NAME, user.getLastName());
        map.put(USER_DISPLAY_NAME, user.getDisplayName());
        map.put(USER_EMAIL_ADDRESS, user.getEmail());

        return map;
    }

    public List<String> getAllGroups()
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getAllGroups");

        return groups.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getAllUsers()
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getAllUsers");

        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
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

        return groups.stream().filter(group -> {

            return attributeMap.entrySet().stream().allMatch(entry -> {

                if (entry.getKey().equalsIgnoreCase(GROUP_ID))
                    return group.getId().equalsIgnoreCase(entry.getValue());
                else if (entry.getKey().equalsIgnoreCase(GROUP_DESCRIPTION))
                    return group.getDescription().equalsIgnoreCase(entry.getValue());
                else
                    return false;
            });
        })
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getUsersByAttributes(Map<String, String> attributeMap)
            throws DirectoryAccessFailureException, SecurityProblemException {

        logger.info("Call: getUsersByAttributes");

        if (attributeMap.isEmpty())
            return getAllUsers();

        return users.stream().filter(user -> {

            return attributeMap.entrySet().stream().allMatch(entry -> {

                if (entry.getKey().equalsIgnoreCase(USER_ID))
                    return user.getId().equalsIgnoreCase(entry.getValue());
                else if (entry.getKey().equalsIgnoreCase(USER_FIRST_NAME))
                    return user.getFirstName().equalsIgnoreCase(entry.getValue());
                else if (entry.getKey().equalsIgnoreCase(USER_LAST_NAME))
                    return user.getLastName().equalsIgnoreCase(entry.getValue());
                else if (entry.getKey().equalsIgnoreCase(USER_DISPLAY_NAME))
                    return user.getDisplayName().equalsIgnoreCase(entry.getValue());
                else if (entry.getKey().equalsIgnoreCase(USER_EMAIL_ADDRESS))
                    return user.getEmail().equalsIgnoreCase(entry.getValue());
                else
                    return false;
            });
        })
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectUsersOfGroup; id={}", id);

        Group group = findGroupById(id);

        return group.userMembers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectGroupsOfUser; id={}", id);

        User user = findUserById(id);

        return groups.stream()
                .filter(x -> x.getUserMembers().contains(user))
                .map(Group::getId)
                .collect(Collectors.toList());
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

        Group group = findGroupById(id);

        return group.groupMembers.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getDirectParentGroupsOfGroup; id={}", id);

        Group group = findGroupById(id);

        return groups.stream()
                .filter(x -> x.getGroupMembers().contains(group))
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveChildGroupsOfGroup; id={}", id);

        List<Group> acc = new ArrayList<>();
        Group group = findGroupById(id);

        acc.add(group);
        resolveGroupsDownwards(group, acc);
        acc.remove(group);

        return acc.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: getTransitiveParentGroupsOfGroup; id={}", id);

        List<Group> acc = new ArrayList<>();
        Group group = findGroupById(id);

        acc.add(group);
        resolveGroupsUpwards(group, acc);
        acc.remove(group);

        return acc.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    private void resolveGroupsDownwards(Group group, List<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        List<Group> result = group.getGroupMembers();

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsDownwards(x, acc);
    }

    private void resolveGroupsUpwards(Group group, List<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        List<Group> result =
                groups.stream()
                        .filter(x -> x.getGroupMembers().contains(group))
                        .collect(Collectors.toList());

        result.removeAll(acc);
        acc.addAll(result);

        for (Group x : result)
            resolveGroupsUpwards(x, acc);
    }

    public boolean isGroupDirectGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isGroupDirectGroupMember");

        findGroupById(groupId1);

        return getDirectChildGroupsOfGroup(groupId2).stream()
                .anyMatch(x -> x.equalsIgnoreCase(groupId1));
    }

    public boolean isUserDirectGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isUserDirectGroupMember");

        findUserById(userId);

        return getDirectUsersOfGroup(groupId).stream()
                .anyMatch(x -> x.equalsIgnoreCase(userId));
    }

    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isGroupTransitiveGroupMember");

        findGroupById(groupId1);

        return getTransitiveParentGroupsOfGroup(groupId2).contains(groupId1);
    }

    public boolean isUserTransitiveGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        logger.info("Call: isUserTransitiveGroupMember");

        findUserById(userId);

        return getTransitiveUsersOfGroup(groupId).contains(userId);
    }

    private static class Group {

        private final String id;
        private final String description;
        private final List<Group> groupMembers = new ArrayList<>();
        private final List<User> userMembers = new ArrayList<>();

        /**
         * Instantiates a new Group.
         *
         * @param id          the id
         * @param description the description
         */
        public Group(String id, String description) {

            this.id = id;
            this.description = description;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj)
                return true;

            if (obj == null)
                return false;

            if (!(obj instanceof Group))
                return false;

            return Objects.equals(id, ((Group) obj).id);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id);
        }

        /**
         * Gets id.
         *
         * @return the id
         */
        public String getId() {

            return id;
        }

        /**
         * Gets description.
         *
         * @return the description
         */
        public String getDescription() {

            return description;
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

    private static class User {

        private final String id;
        private final String firstName;
        private final String lastName;
        private final String displayName;
        private final String email;
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
         */
        public User(String id, String firstName, String lastName, String displayName, String email, String password) {

            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.displayName = displayName;
            this.email = email;
            this.password = password;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj)
                return true;

            if (obj == null)
                return false;

            if (!(obj instanceof User))
                return false;

            return Objects.equals(id, ((User) obj).id);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id);
        }

        /**
         * Gets id.
         *
         * @return the id
         */
        public String getId() {

            return id;
        }

        /**
         * Gets first name.
         *
         * @return the first name
         */
        public String getFirstName() {

            return firstName;
        }

        /**
         * Gets last name.
         *
         * @return the last name
         */
        public String getLastName() {

            return lastName;
        }

        /**
         * Gets display name.
         *
         * @return the display name
         */
        public String getDisplayName() {

            return displayName;
        }

        /**
         * Gets email.
         *
         * @return the email
         */
        public String getEmail() {

            return email;
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
