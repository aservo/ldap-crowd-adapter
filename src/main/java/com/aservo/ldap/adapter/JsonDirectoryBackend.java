package com.aservo.ldap.adapter;

import com.aservo.ldap.adapter.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.exception.EntryNotFoundException;
import com.aservo.ldap.adapter.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.DirectoryBackend;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


public class JsonDirectoryBackend
        implements DirectoryBackend {

    private final String id;
    private final List<Group> groups = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    private final File dbFile;

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

            JsonObject jsonObject = gson.fromJson(new FileReader(dbFile), JsonObject.class);

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

                String id = x.getAsJsonObject().get("id").getAsString();
                JsonArray groupMemberNode = x.getAsJsonObject().getAsJsonArray("group_members");
                JsonArray userMemberNode = x.getAsJsonObject().getAsJsonArray("user_members");

                Group group = groups.stream().filter((z) -> z.getId().equals(id)).findAny().get();

                for (JsonElement y : groupMemberNode)
                    group.addGroup(groups.stream().filter((z) -> z.getId().equals(y.getAsString())).findAny().get());

                for (JsonElement y : userMemberNode)
                    group.addUser(users.stream().filter((z) -> z.getId().equals(y.getAsString())).findAny().get());
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
                .filter((x) -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntryNotFoundException("Cannot find group with id " + id));
    }

    private User findUserById(String id)
            throws EntryNotFoundException {

        return users.stream()
                .filter((x) -> x.getId().equalsIgnoreCase(id))
                .findAny()
                .orElseThrow(() -> new EntryNotFoundException("Cannot find user with id " + id));
    }

    public Map<String, String> getGroupInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        Group group = findGroupById(id);

        return mapGroupInfo(group);
    }

    public Map<String, String> getUserInfo(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        User user = findUserById(id);

        return mapUserInfo(user);
    }

    public Map<String, String> getInfoFromAuthenticatedUser(String id, String password)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

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

        return groups.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getAllUsers()
            throws DirectoryAccessFailureException, SecurityProblemException {

        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getGroupsByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException {

        return groups.stream().filter((x) -> {

            if (attribute.equalsIgnoreCase(GROUP_ID))
                return x.getId().equalsIgnoreCase(value);
            else if (attribute.equalsIgnoreCase(GROUP_DESCRIPTION))
                return x.getDescription().equalsIgnoreCase(value);
            else
                return false;
        })
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getUsersByAttribute(String attribute, String value)
            throws DirectoryAccessFailureException, SecurityProblemException {

        return users.stream().filter((x) -> {

            if (attribute.equalsIgnoreCase(USER_ID))
                return x.getId().equalsIgnoreCase(value);
            else if (attribute.equalsIgnoreCase(USER_FIRST_NAME))
                return x.getFirstName().equalsIgnoreCase(value);
            else if (attribute.equalsIgnoreCase(USER_LAST_NAME))
                return x.getLastName().equalsIgnoreCase(value);
            else if (attribute.equalsIgnoreCase(USER_DISPLAY_NAME))
                return x.getDisplayName().equalsIgnoreCase(value);
            else if (attribute.equalsIgnoreCase(USER_EMAIL_ADDRESS))
                return x.getEmail().equalsIgnoreCase(value);
            else
                return false;
        })
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        Group group = findGroupById(id);

        return group.userMembers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        User user = findUserById(id);

        return groups.stream()
                .filter((x) -> x.getUserMembers().contains(user))
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveUsersOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        List<String> userIds = getDirectUsersOfGroup(id);

        for (String y : getTransitiveChildGroupsOfGroup(id))
            for (String x : getDirectUsersOfGroup(y))
                if (!userIds.contains(x))
                    userIds.add(x);

        return userIds;
    }

    public List<String> getTransitiveGroupsOfUser(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        List<String> groupIds = getDirectGroupsOfUser(id);

        for (String y : new ArrayList<>(groupIds))
            for (String x : getTransitiveParentGroupsOfGroup(y))
                if (!groupIds.contains(x))
                    groupIds.add(x);

        return groupIds;
    }

    public List<String> getDirectChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        Group group = findGroupById(id);

        return group.groupMembers.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getDirectParentGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        Group group = findGroupById(id);

        return groups.stream()
                .filter((x) -> x.getGroupMembers().contains(group))
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    public List<String> getTransitiveChildGroupsOfGroup(String id)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

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

        for (Group x : result)
            if (!acc.contains(x))
                acc.add(x);

        for (Group x : result)
            resolveGroupsDownwards(x, acc);
    }

    private void resolveGroupsUpwards(Group group, List<Group> acc)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        List<Group> result =
                groups.stream()
                        .filter((x) -> x.getGroupMembers().contains(group))
                        .collect(Collectors.toList());

        for (Group x : result)
            if (!acc.contains(x))
                acc.add(x);

        for (Group x : result)
            resolveGroupsUpwards(x, acc);
    }

    public boolean isGroupDirectGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        findGroupById(groupId1);

        return getDirectChildGroupsOfGroup(groupId2).stream()
                .anyMatch((x) -> x.equalsIgnoreCase(groupId1));
    }

    public boolean isUserDirectGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        findUserById(userId);

        return getDirectUsersOfGroup(groupId).stream()
                .anyMatch((x) -> x.equalsIgnoreCase(userId));
    }

    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        findGroupById(groupId1);

        return getTransitiveParentGroupsOfGroup(groupId2).contains(groupId1);
    }

    public boolean isUserTransitiveGroupMember(String userId, String groupId)
            throws DirectoryAccessFailureException, SecurityProblemException, EntryNotFoundException {

        findUserById(userId);

        return getTransitiveUsersOfGroup(groupId).contains(userId);
    }

    private static class Group {

        private final String id;
        private final String description;
        private final List<Group> groupMembers = new ArrayList<>();
        private final List<User> userMembers = new ArrayList<>();

        public Group(String id, String description) {

            this.id = id;
            this.description = description;
        }

        public String getId() {

            return id;
        }

        public String getDescription() {

            return description;
        }

        public void addGroup(Group group) {

            groupMembers.add(group);
        }

        public void addUser(User user) {

            userMembers.add(user);
        }

        public List<Group> getGroupMembers() {

            return new ArrayList<>(groupMembers);
        }

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

        public User(String id, String firstName, String lastName, String displayName, String email, String password) {

            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.displayName = displayName;
            this.email = email;
            this.password = password;
        }

        public String getId() {

            return id;
        }

        public String getFirstName() {

            return firstName;
        }

        public String getLastName() {

            return lastName;
        }

        public String getDisplayName() {

            return displayName;
        }

        public String getEmail() {

            return email;
        }

        public String getPassword() {

            return password;
        }
    }
}
