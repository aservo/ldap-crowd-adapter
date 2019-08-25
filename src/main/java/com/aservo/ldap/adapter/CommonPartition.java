/*
 * Initiator:
 * Copyright (c) 2012 Dieter Wimberger
 * http://dieter.wimpi.net
 *
 * Maintenance:
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
import com.aservo.ldap.adapter.util.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonPartition
        extends SimpleReadOnlyPartition {

    private final Logger logger = LoggerFactory.getLogger(CommonPartition.class);

    private final DirectoryBackend directoryBackend;
    private final ServerConfiguration serverConfig;
    private LRUCacheMap<String, Entry> entryCache;
    private FilterMatcher filterProcessor;

    private Entry rootEntry;
    private Entry groupsEntry;
    private Entry usersEntry;

    public CommonPartition(DirectoryBackend directoryBackend, ServerConfiguration serverConfig) {

        super(directoryBackend.getId());

        this.directoryBackend = directoryBackend;
        this.serverConfig = serverConfig;
        this.entryCache = new LRUCacheMap<>(serverConfig.getEntryCacheMaxSize());
    }

    @Override
    protected void doInit()
            throws Exception {

        // create root entry
        // dn: dc=<domain>
        // objectclass: top
        // objectclass: domain
        // description: <id> Domain
        Dn rootDn = new Dn(schemaManager, directoryBackend.getRootDnString());
        rootEntry = new DefaultEntry(schemaManager, rootDn);
        rootEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.DOMAIN_OC);
        rootEntry.put(SchemaConstants.DC_AT, rootDn.getRdn().getAva().getValue().toString());
        rootEntry.put(SchemaConstants.DESCRIPTION_AT, directoryBackend.getId() + " Domain");

        // create groups entry
        // dn: ou=groups, dc=<domain>
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: groups
        // description: <id> Groups
        Dn groupDn = new Dn(schemaManager, directoryBackend.getGroupDnString());
        groupsEntry = new DefaultEntry(schemaManager, groupDn);
        groupsEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        groupsEntry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
        groupsEntry.put(SchemaConstants.DESCRIPTION_AT, directoryBackend.getId() + " Groups");

        // create users entry
        // dn: ou=users, dc=<domain>
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: users
        // description: <id> Users
        Dn usersDn = new Dn(schemaManager, directoryBackend.getUserDnString());
        usersEntry = new DefaultEntry(schemaManager, usersDn);
        usersEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        usersEntry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
        usersEntry.put(SchemaConstants.DESCRIPTION_AT, directoryBackend.getId() + " Users");

        // add to cache
        if (serverConfig.isEntryCacheEnabled()) {

            entryCache.put(rootDn.getName(), rootEntry);
            entryCache.put(groupDn.getName(), groupsEntry);
            entryCache.put(usersDn.getName(), usersEntry);
        }

        filterProcessor =
                new FilterMatcher() {

                    @Override
                    protected List<String> getValuesFromAttribute(String attribute, String entryId, OuType ouType) {

                        if (ouType.equals(OuType.GROUP))
                            return getGroupValueFromAttribute(attribute, entryId);
                        else if (ouType.equals(OuType.USER))
                            return getUserValueFromAttribute(attribute, entryId);

                        return null;
                    }

                    @Nullable
                    @Override
                    protected String getGroupFromDn(@Nullable String value) {

                        if (value == null)
                            return null;

                        try {

                            return LdapHelper.getGroupFromDn(rootDn, groupDn, new Dn(schemaManager, value));

                        } catch (LdapInvalidDnException e) {

                            return null;
                        }
                    }

                    @Nullable
                    @Override
                    protected String getUserFromDn(@Nullable String value) {

                        if (value == null)
                            return null;

                        try {

                            return LdapHelper.getUserFromDn(rootDn, usersDn, new Dn(schemaManager, value));

                        } catch (LdapInvalidDnException e) {

                            return null;
                        }
                    }
                };
    }

    @Override
    protected void doDestroy()
            throws Exception {

        logger.info("Destroy partition: {}", id);
        directoryBackend.shutdown();
    }

    @Override
    public Dn getSuffixDn() {

        return rootEntry.getDn();
    }

    private List<String> getGroupValueFromAttribute(String attribute, String groupId) {

        try {

            Map<String, String> groupInfo = directoryBackend.getGroupInfo(groupId);

            switch (Utils.normalizeAttribute(attribute)) {

                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    return Utils.nullableSingletonList(groupInfo.get(DirectoryBackend.GROUP_ID));

                case SchemaConstants.DESCRIPTION_AT:

                    return Utils.nullableSingletonList(groupInfo.get(DirectoryBackend.GROUP_DESCRIPTION));

                case SchemaConstants.MEMBER_AT:
                case SchemaConstants.MEMBER_AT_OID:
                case SchemaConstants.UNIQUE_MEMBER_AT:
                case SchemaConstants.UNIQUE_MEMBER_AT_OID:

                    return findMembers(groupId);

                default:
                    return Collections.emptyList();
            }

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.error("Cannot get value of group attribute.", e);
            return Collections.emptyList();
        }
    }

    private List<String> getUserValueFromAttribute(String attribute, String userId) {

        try {

            Map<String, String> userInfo = directoryBackend.getUserInfo(userId);

            switch (Utils.normalizeAttribute(attribute)) {

                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    return Utils.nullableSingletonList(userInfo.get(DirectoryBackend.USER_ID));

                case SchemaConstants.GN_AT:
                case SchemaConstants.GN_AT_OID:
                case SchemaConstants.GIVENNAME_AT:

                    return Utils.nullableSingletonList(userInfo.get(DirectoryBackend.USER_FIRST_NAME));

                case SchemaConstants.SN_AT:
                case SchemaConstants.SN_AT_OID:
                case SchemaConstants.SURNAME_AT:

                    return Utils.nullableSingletonList(userInfo.get(DirectoryBackend.USER_LAST_NAME));

                case SchemaConstants.DISPLAY_NAME_AT:
                case SchemaConstants.DISPLAY_NAME_AT_OID:

                    return Utils.nullableSingletonList(userInfo.get(DirectoryBackend.USER_DISPLAY_NAME));

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    return Utils.nullableSingletonList(userInfo.get(DirectoryBackend.USER_EMAIL_ADDRESS));

                case Utils.MEMBER_OF_AT:

                    return findGroupsForMemberOf(userId);

                default:
                    return Collections.emptyList();
            }

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.error("Cannot get value of user attribute.", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    private Entry createGroupEntry(Dn dn) {

        Entry entry = entryCache.get(dn.getName());

        if (entry != null)
            return entry;

        try {

            String groupId = dn.getRdn().getNormValue();
            Map<String, String> groupInfo = directoryBackend.getGroupInfo(groupId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
            entry.put(SchemaConstants.CN_AT, groupInfo.get(DirectoryBackend.GROUP_ID));
            entry.put(SchemaConstants.DESCRIPTION_AT, groupInfo.get(DirectoryBackend.GROUP_DESCRIPTION));

            List<Dn> userDns = findMembers(groupId).stream()
                    .map(this::createUserDn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (Dn userDn : userDns)
                entry.add(SchemaConstants.MEMBER_AT, userDn.getName());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(dn.getName(), entry);

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not create group entry because of problems with directory query.", e);
            return null;

        } catch (LdapException e) {

            logger.debug("Could not create group entry because of an incorrect build.", e);
            return null;
        }

        return entry;
    }

    @Nullable
    private Entry createUserEntry(Dn dn) {

        Entry entry = entryCache.get(dn.getName());

        if (entry != null)
            return entry;

        try {

            String userId = dn.getRdn().getNormValue();
            Map<String, String> userInfo = directoryBackend.getUserInfo(userId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ORGANIZATIONAL_PERSON_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.INET_ORG_PERSON_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
            entry.put(SchemaConstants.UID_AT, userInfo.get(DirectoryBackend.USER_ID));
            entry.put(SchemaConstants.CN_AT, userInfo.get(DirectoryBackend.USER_ID));
            entry.put(SchemaConstants.COMMON_NAME_AT, userInfo.get(DirectoryBackend.USER_ID));
            entry.put(SchemaConstants.GN_AT, userInfo.get(DirectoryBackend.USER_FIRST_NAME));
            entry.put(SchemaConstants.GIVENNAME_AT, userInfo.get(DirectoryBackend.USER_FIRST_NAME));
            entry.put(SchemaConstants.SN_AT, userInfo.get(DirectoryBackend.USER_LAST_NAME));
            entry.put(SchemaConstants.SURNAME_AT, userInfo.get(DirectoryBackend.USER_LAST_NAME));
            entry.put(SchemaConstants.DISPLAY_NAME_AT, userInfo.get(DirectoryBackend.USER_DISPLAY_NAME));
            entry.put(SchemaConstants.MAIL_AT, userInfo.get(DirectoryBackend.USER_EMAIL_ADDRESS));
            entry.put(SchemaConstants.UID_NUMBER_AT, Utils.calculateHash(userId).toString());

            List<Dn> groupDns = findGroupsForMemberOf(userId).stream()
                    .map(this::createGroupDn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (Dn groupDn : groupDns)
                entry.add(Utils.MEMBER_OF_AT, groupDn.getName());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(dn.getName(), entry);

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not create user entry because of problems with directory query.", e);
            return null;

        } catch (LdapException e) {

            logger.debug("Could not create user entry because of an incorrect build.", e);
            return null;
        }

        return entry;
    }

    private List<String> findMembers(String groupId) {

        try {

            List<String> userIds = new ArrayList<>();
            List<String> groupIds = new ArrayList<>();

            groupIds.add(groupId);

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING))
                resolveGroupsDownwards(groupId, groupIds);

            for (String y : groupIds)
                for (String x : directoryBackend.getUsersOfGroup(y))
                    if (!userIds.contains(x))
                        userIds.add(x);

            return userIds;

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect group members because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    private List<String> findGroupsForMemberOf(String userId) {

        try {

            List<String> groupIds = new LinkedList<>();

            if (serverConfig.getMemberOfSupport().allowMemberOfAttribute()) {

                groupIds.addAll(directoryBackend.getGroupsOfUser(userId));

                if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.NESTED_GROUPS) ||
                        serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING)) {

                    for (String x : new ArrayList<>(groupIds))
                        resolveGroupsUpwards(x, groupIds);
                }
            }

            return groupIds;

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect groups for a member because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    private void resolveGroupsDownwards(String groupId, List<String> accu)
            throws DirectoryAccessFailureException, EntryNotFoundException, SecurityProblemException {

        List<String> result = directoryBackend.getChildGroups(groupId);

        for (String x : result)
            if (!accu.contains(x))
                accu.add(x);

        for (String x : result)
            resolveGroupsDownwards(x, accu);
    }

    private void resolveGroupsUpwards(String groupId, List<String> accu)
            throws DirectoryAccessFailureException, EntryNotFoundException, SecurityProblemException {

        List<String> result = directoryBackend.getParentGroups(groupId);

        for (String x : result)
            if (!accu.contains(x))
                accu.add(x);

        for (String x : result)
            resolveGroupsUpwards(x, accu);
    }

    @Override
    public ClonedServerEntry lookup(LookupOperationContext context) {

        if (!serverConfig.isEntryCacheEnabled())
            return null;

        Dn dn = context.getDn();
        Entry se = entryCache.get(context.getDn().getName());
        if (se == null) {
            //todo
            logger.debug("Could not find cached entry for {}", dn.getName());
            return null;
        } else {
            logger.debug("Could find cached entry for {}", dn.getName());
            return new ClonedServerEntry(se);
        }
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        if (!serverConfig.isEntryCacheEnabled())
            return false;

        Dn dn = context.getDn();

        if (entryCache.containsKey(context.getDn().getName())) {
            return true;
        }

        int dnSize = dn.size();

        // one level in DN
        if (dnSize == 1) {
            if (rootEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), rootEntry);
                return true;
            }

            return false;
        }

        // two levels in DN
        if (dnSize == 2) {
            if (groupsEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), groupsEntry);
                return true;
            }
            if (usersEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), usersEntry);
                return true;
            }
            return false;
        }

        // 3 levels in DN
        if (dnSize == 3) {
            Dn prefix = dn.getParent();
            try {
                prefix.apply(schemaManager);
            } catch (Exception ex) {
                logger.error("hasEntry()", ex);
            }
            logger.debug("Prefix={}", prefix);

            if (usersEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String user = rdn.getNormValue();
                logger.debug("user={}", user);
                Entry userEntry = createUserEntry(dn);
                return (userEntry != null);
            }

            if (groupsEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String group = rdn.getNormValue();
                logger.debug("group={}", group);
                Entry groupEntry = createGroupEntry(dn);
                return (groupEntry != null);
            }

            logger.debug("Prefix is neither users nor groups");
            logger.debug("Users = {}", usersEntry.getDn());
            logger.debug("Groups = {}", groupsEntry.getDn().toString());
            return false;
        }

        return false;
    }

    @Nullable
    private Dn createGroupDn(String groupId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", groupId, directoryBackend.getGroupDnString()));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create group DN.", e);
            return null;
        }
    }

    @Nullable
    private Dn createUserDn(String userId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", userId, directoryBackend.getUserDnString()));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create user DN.", e);
            return null;
        }
    }

    private List<String> findGroups() {

        try {

            return directoryBackend.getGroups();

        } catch (SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Cannot receive group information from directory backend.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findGroup(String attribute, String value) {

        List<String> result;

        try {

            switch (Utils.normalizeAttribute(attribute)) {

                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    result = directoryBackend.getGroupsByAttribute(DirectoryBackend.GROUP_ID, value);

                    break;

                default:

                    logger.debug("Cannot handle unknown attribute : " + attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique group for attribute: " + attribute);
                return null;
            }

            return result.stream().findAny().orElse(null);

        } catch (SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.error("Cannot receive group information from directory backend.", e);

            return null;
        }
    }

    private List<String> findUsers() {

        try {

            return directoryBackend.getUsers();

        } catch (SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.error("Cannot receive user information from directory backend.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findUser(String attribute, String value) {

        List<String> result;

        try {

            switch (Utils.normalizeAttribute(attribute)) {

                case SchemaConstants.UID_NUMBER_AT:
                case SchemaConstants.UID_NUMBER_AT_OID:

                    result = directoryBackend.getUsers().stream()
                            .filter((x) -> Utils.calculateHash(x).toString().equals(value))
                            .collect(Collectors.toList());

                    break;

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    result = directoryBackend.getUsersByAttribute(DirectoryBackend.USER_ID, value);

                    break;

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    result = directoryBackend.getUsersByAttribute(DirectoryBackend.USER_EMAIL_ADDRESS, value);

                    break;

                default:

                    logger.warn("Cannot handle unknown attribute : " + attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique user for attribute: " + attribute);
                return null;
            }

            return result.stream().findAny().orElse(null);

        } catch (SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.error("Cannot receive user information from directory backend.", e);

            return null;
        }
    }

    private List<Entry> createGroupEntryList(List<String> groupIds, ExprNode filter) {

        List<Entry> entries =
                groupIds.stream()
                        .filter((x) -> filterProcessor.match(filter, x, OuType.GROUP))
                        .map(this::createGroupDn)
                        .filter(Objects::nonNull)
                        .map(this::createGroupEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        for (Entry x : entries)
            logger.debug("Return with group \n{}", x);

        return entries;
    }

    private List<Entry> createUserEntryList(List<String> userIds, ExprNode filter) {

        List<Entry> entries =
                userIds.stream()
                        .filter((x) -> filterProcessor.match(filter, x, OuType.USER))
                        .map(this::createUserDn)
                        .filter(Objects::nonNull)
                        .map(this::createUserEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        for (Entry x : entries)
            logger.debug("Return with user \n{}", x);

        return entries;
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context) {

        logger.debug("Try to find one entry with dn={} and filter={}",
                context.getDn().getName(),
                context.getFilter());

        if (context.getDn().getParent().equals(groupsEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntries = createGroupEntryList(groupIds, context.getFilter());

            return groupEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().getParent().equals(usersEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return userEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().getParent().equals(rootEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();
            List<Entry> mergedEntries = new LinkedList<>();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));

            if (!userIds.isEmpty())
                mergedEntries.addAll(createUserEntryList(userIds, context.getFilter()));
            else {

                List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));

                if (!groupIds.isEmpty())
                    mergedEntries.addAll(createGroupEntryList(groupIds, context.getFilter()));
            }

            return mergedEntries.stream().findAny()
                    .map((entry) -> {

                        return new EntryFilteringCursorImpl(
                                new SingletonCursor<>(entry),
                                context,
                                schemaManager);
                    })
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        logger.debug("Try to find many entries on first level with dn={} and filter={}",
                context.getDn().getName(),
                context.getFilter());

        if (context.getDn().equals(groupsEntry.getDn())) {

            List<Entry> groupEntryList = createGroupEntryList(findGroups(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(groupsEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntryList = createGroupEntryList(groupIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(usersEntry.getDn())) {

            List<Entry> userEntries = createUserEntryList(findUsers(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(usersEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(rootEntry.getDn())) {

            List<Entry> groupEntries = createGroupEntryList(findGroups(), context.getFilter());
            List<Entry> userEntries = createUserEntryList(findUsers(), context.getFilter());

            List<Entry> mergedEntries =
                    Stream.concat(groupEntries.stream(), userEntries.stream())
                            .collect(Collectors.toList());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(rootEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();
            List<Entry> mergedEntries = new LinkedList<>();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));

            if (!userIds.isEmpty())
                mergedEntries.addAll(createUserEntryList(userIds, context.getFilter()));
            else {

                List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));

                if (!groupIds.isEmpty())
                    mergedEntries.addAll(createGroupEntryList(groupIds, context.getFilter()));
            }

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        logger.debug("Try to find many entries on multiple levels with dn={} and filter={}",
                context.getDn().getName(),
                context.getFilter());

        // will only search at one level
        return findManyOnFirstLevel(context);
    }
}
