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
    private final Map<Dn, Entry> entryCache;
    private FilterMatcher filterProcessor;

    private Dn rootDn;
    private Dn groupsDn;
    private Dn usersDn;

    public CommonPartition(DirectoryBackend directoryBackend, ServerConfiguration serverConfig) {

        super(directoryBackend.getId());

        this.directoryBackend = directoryBackend;
        this.serverConfig = serverConfig;

        this.entryCache =
                Collections.synchronizedMap(
                        new LruCacheMap<>(serverConfig.getEntryCacheMaxSize(), serverConfig.getEntryCacheMaxAge())
                );
    }

    @Override
    protected void doInit()
            throws LdapException {

        rootDn = LdapHelper.createRootDn(schemaManager, directoryBackend.getId());
        groupsDn = LdapHelper.createGroupsDn(schemaManager, directoryBackend.getId());
        usersDn = LdapHelper.createUsersDn(schemaManager, directoryBackend.getId());

        filterProcessor =
                new FilterMatcher() {

                    @Override
                    protected List<String> getValuesFromAttribute(String attribute,
                                                                  String entryId,
                                                                  EntryType entryType) {

                        if (entryType.equals(EntryType.DOMAIN))
                            return getDomainValueFromAttribute(attribute, entryId);
                        else if (entryType.equals(EntryType.UNIT))
                            return getUnitValueFromAttribute(attribute, entryId);
                        else if (entryType.equals(EntryType.GROUP))
                            return getGroupValueFromAttribute(attribute, entryId);
                        else if (entryType.equals(EntryType.USER))
                            return getUserValueFromAttribute(attribute, entryId);

                        return Collections.emptyList();
                    }

                    @Nullable
                    @Override
                    protected String getGroupFromDn(@Nullable String value) {

                        if (value == null)
                            return null;

                        try {

                            return LdapHelper.getGroupFromDn(rootDn, groupsDn, new Dn(schemaManager, value));

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
            throws LdapException {

        entryCache.clear();
    }

    @Override
    public Dn getSuffixDn() {

        return rootDn;
    }

    private List<String> getDomainValueFromAttribute(String attribute, String entryId) {

        switch (Utils.normalizeAttribute(attribute)) {

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entryId.equalsIgnoreCase(getId()))
                    return Utils.nullableSingletonList(serverConfig.getBaseDnDescription());

                break;

            default:
                break;
        }

        return Collections.emptyList();
    }

    private List<String> getUnitValueFromAttribute(String attribute, String entryId) {

        switch (Utils.normalizeAttribute(attribute)) {

            case SchemaConstants.DESCRIPTION_AT:
            case SchemaConstants.DESCRIPTION_AT_OID:

                if (entryId.equalsIgnoreCase(Utils.OU_GROUPS))
                    return Utils.nullableSingletonList(serverConfig.getBaseDnGroupsDescription());

                if (entryId.equalsIgnoreCase(Utils.OU_USERS))
                    return Utils.nullableSingletonList(serverConfig.getBaseDnUsersDescription());

                break;

            default:
                break;
        }

        return Collections.emptyList();
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

                    return collectMemberDn(groupId);

                case Utils.MEMBER_OF_AT:

                    return collectMemberOfDnForUser(groupId);

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

                    return collectMemberOfDnForUser(userId);

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

    private Entry createSystemEntry(Dn dn) {

        Entry entry = entryCache.get(dn);

        if (entry != null)
            return entry;

        if (dn.equals(rootDn)) {

            // create root entry
            // dn: dc=<domain>
            // objectclass: top
            // objectclass: domain
            // description: <id> Domain

            entry = new DefaultEntry(schemaManager, rootDn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.DOMAIN_OC);
            entry.put(SchemaConstants.DC_AT, rootDn.getRdn().getAva().getValue().toString());
            entry.put(SchemaConstants.DESCRIPTION_AT, serverConfig.getBaseDnDescription());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(rootDn, entry);

        } else if (dn.equals(groupsDn)) {

            // create groups entry
            // dn: ou=groups, dc=<domain>
            // objectClass: top
            // objectClass: organizationalUnit
            // ou: groups
            // description: <id> Groups

            entry = new DefaultEntry(schemaManager, groupsDn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.ORGANIZATIONAL_UNIT_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
            entry.put(SchemaConstants.DESCRIPTION_AT, serverConfig.getBaseDnGroupsDescription());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(groupsDn, entry);

        } else if (dn.equals(usersDn)) {

            // create users entry
            // dn: ou=users, dc=<domain>
            // objectClass: top
            // objectClass: organizationalUnit
            // ou: users
            // description: <id> Users

            entry = new DefaultEntry(schemaManager, usersDn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.ORGANIZATIONAL_UNIT_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
            entry.put(SchemaConstants.DESCRIPTION_AT, serverConfig.getBaseDnUsersDescription());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(usersDn, entry);

        } else
            throw new IllegalArgumentException("Cannot create system entry with DN=" + dn);

        return entry;
    }

    @Nullable
    private Entry createGroupEntry(Dn dn) {

        Entry entry = entryCache.get(dn);

        if (entry != null)
            return entry;

        try {

            String groupId = dn.getRdn().getNormValue();
            Map<String, String> groupInfo = directoryBackend.getGroupInfo(groupId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.GROUP_OF_NAMES_OC,
                    SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
            entry.put(SchemaConstants.CN_AT, groupInfo.get(DirectoryBackend.GROUP_ID));
            entry.put(SchemaConstants.DESCRIPTION_AT, groupInfo.get(DirectoryBackend.GROUP_DESCRIPTION));

            for (String memberDn : collectMemberDn(groupId))
                entry.add(SchemaConstants.MEMBER_AT, memberDn);

            for (String memberOfDn : collectMemberOfDnForGroup(groupId))
                entry.add(Utils.MEMBER_OF_AT, memberOfDn);

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(dn, entry);

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

        Entry entry = entryCache.get(dn);

        if (entry != null)
            return entry;

        try {

            String userId = dn.getRdn().getNormValue();
            Map<String, String> userInfo = directoryBackend.getUserInfo(userId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.TOP_OC,
                    SchemaConstants.PERSON_OC,
                    SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                    SchemaConstants.INET_ORG_PERSON_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
            entry.put(SchemaConstants.UID_AT, userInfo.get(DirectoryBackend.USER_ID));
            entry.put(SchemaConstants.CN_AT, userInfo.get(DirectoryBackend.USER_ID));
            entry.put(SchemaConstants.GN_AT, userInfo.get(DirectoryBackend.USER_FIRST_NAME));
            entry.put(SchemaConstants.SN_AT, userInfo.get(DirectoryBackend.USER_LAST_NAME));
            entry.put(SchemaConstants.DISPLAY_NAME_AT, userInfo.get(DirectoryBackend.USER_DISPLAY_NAME));
            entry.put(SchemaConstants.MAIL_AT, userInfo.get(DirectoryBackend.USER_EMAIL_ADDRESS));
            entry.put(SchemaConstants.UID_NUMBER_AT,
                    Integer.toString(Utils.calculateHash(userInfo.get(DirectoryBackend.USER_ID))));

            for (String memberOfDn : collectMemberOfDnForUser(userId))
                entry.add(Utils.MEMBER_OF_AT, memberOfDn);

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(dn, entry);

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

    private List<String> collectMemberDn(String groupId) {

        return Stream.concat(
                findUserMembers(groupId).stream()
                        .map(x -> LdapHelper.createDnWithCn(schemaManager, usersDn, x)),
                findGroupMembers(groupId).stream()
                        .map(x -> LdapHelper.createDnWithCn(schemaManager, groupsDn, x))
        )
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    private List<String> collectMemberOfDnForGroup(String groupId) {

        return findGroupMembersReverse(groupId).stream()
                .map(x -> LdapHelper.createDnWithCn(schemaManager, groupsDn, x))
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    private List<String> collectMemberOfDnForUser(String userId) {

        return findUserMembersReverse(userId).stream()
                .map(x -> LdapHelper.createDnWithCn(schemaManager, groupsDn, x))
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    private List<String> findGroupMembers(String groupId) {

        try {

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.NESTED_GROUPS))
                return directoryBackend.getDirectChildGroupsOfGroup(groupId);

            return Collections.emptyList();

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect group members because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    private List<String> findUserMembers(String groupId) {

        try {

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING))
                return directoryBackend.getTransitiveUsersOfGroup(groupId);

            return directoryBackend.getDirectUsersOfGroup(groupId);

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect user members because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    private List<String> findGroupMembersReverse(String groupId) {

        try {

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.NESTED_GROUPS))
                return directoryBackend.getDirectParentGroupsOfGroup(groupId);

            return Collections.emptyList();

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect groups for a group member because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    private List<String> findUserMembersReverse(String userId) {

        try {

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING))
                return directoryBackend.getTransitiveGroupsOfUser(userId);

            if (serverConfig.getMemberOfSupport().allowMemberOfAttribute())
                return directoryBackend.getDirectGroupsOfUser(userId);

            return Collections.emptyList();

        } catch (EntryNotFoundException |
                SecurityProblemException |
                DirectoryAccessFailureException e) {

            logger.debug("Could not collect groups for an user member because of problems with directory query.", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    @Override
    public ClonedServerEntry lookup(LookupOperationContext context)
            throws LdapException {

        Entry entry = entryCache.get(context.getDn());

        if (entry == null) {

            logger.debug("Could not find cached entry with DN={}", context.getDn().getName());
            return null;

        } else {

            logger.debug("Could find cached entry with DN={}", context.getDn().getName());
            return new ClonedServerEntry(entry);
        }
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        logger.debug("Check for existence of entry with DN={}",
                context.getDn().getName());

        if (entryCache.containsKey(context.getDn()))
            return true;

        if (context.getDn().equals(groupsDn)) {

            return true;

        } else if (context.getDn().getParent().equals(groupsDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            return findGroup(attribute, value) != null;

        } else if (context.getDn().equals(usersDn)) {

            return true;

        } else if (context.getDn().getParent().equals(usersDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            return findUser(attribute, value) != null;

        } else if (context.getDn().equals(rootDn)) {

            return true;

        } else if (context.getDn().getParent().equals(rootDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            String userId = findUser(attribute, value);

            if (userId != null)
                return true;

            String groupId = findGroup(attribute, value);

            if (groupId != null)
                return true;
        }

        return false;
    }

    private List<String> findGroups() {

        try {

            return directoryBackend.getAllGroups();

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

                    logger.debug("Cannot handle unknown attribute : {}", attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique group for attribute: {}", attribute);
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

            return directoryBackend.getAllUsers();

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

                    result = directoryBackend.getAllUsers().stream()
                            .filter(x -> Integer.toString(Utils.calculateHash(x)).equals(value))
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

                    logger.warn("Cannot handle unknown attribute : {}", attribute);
                    return null;
            }

            if (result.size() > 1) {

                logger.error("Expect unique user for attribute: {}", attribute);
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
                        .filter(x -> filterProcessor.match(filter, x, EntryType.GROUP))
                        .map(x -> LdapHelper.createDnWithCn(schemaManager, groupsDn, x))
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
                        .filter(x -> filterProcessor.match(filter, x, EntryType.USER))
                        .map(x -> LdapHelper.createDnWithCn(schemaManager, usersDn, x))
                        .map(this::createUserEntry)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        for (Entry x : entries)
            logger.debug("Return with user \n{}", x);

        return entries;
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context) {

        logger.info("[{}] - Access partition: DN={} filter={} scope={}",
                context.getSession().getClientAddress(),
                context.getDn().getName(),
                context.getFilter(),
                context.getScope());

        if (context.getDn().equals(groupsDn)) {

            if (!filterProcessor.match(context.getFilter(), Utils.OU_GROUPS, EntryType.UNIT))
                return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);

            return new EntryFilteringCursorImpl(
                    new SingletonCursor<>(createSystemEntry(groupsDn)),
                    context,
                    schemaManager);

        } else if (context.getDn().getParent().equals(groupsDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntries = createGroupEntryList(groupIds, context.getFilter());

            return groupEntries.stream().findAny()
                    .map(entry -> new EntryFilteringCursorImpl(
                            new SingletonCursor<>(entry),
                            context,
                            schemaManager))
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().equals(usersDn)) {

            if (!filterProcessor.match(context.getFilter(), Utils.OU_USERS, EntryType.UNIT))
                return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);

            return new EntryFilteringCursorImpl(
                    new SingletonCursor<>(createSystemEntry(usersDn)),
                    context,
                    schemaManager);

        } else if (context.getDn().getParent().equals(usersDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return userEntries.stream().findAny()
                    .map(entry -> new EntryFilteringCursorImpl(
                            new SingletonCursor<>(entry),
                            context,
                            schemaManager))
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));

        } else if (context.getDn().equals(rootDn)) {

            if (!filterProcessor.match(context.getFilter(), getId(), EntryType.DOMAIN))
                return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);

            return new EntryFilteringCursorImpl(
                    new SingletonCursor<>(createSystemEntry(rootDn)),
                    context,
                    schemaManager);

        } else if (context.getDn().getParent().equals(rootDn)) {

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
                    .map(entry -> new EntryFilteringCursorImpl(
                            new SingletonCursor<>(entry),
                            context,
                            schemaManager))
                    .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));
        }

        // return an empty result
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        logger.info("[{}] - Access partition: DN={} filter={} scope={}",
                context.getSession().getClientAddress(),
                context.getDn().getName(),
                context.getFilter(),
                context.getScope());

        if (context.getDn().equals(groupsDn)) {

            List<Entry> mergedEntries = Utils.nullableOneElementList(createSystemEntry(groupsDn));

            if (!filterProcessor.match(context.getFilter(), Utils.OU_GROUPS, EntryType.UNIT))
                mergedEntries.remove(0);

            mergedEntries.addAll(createGroupEntryList(findGroups(), context.getFilter()));

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(groupsDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntryList = createGroupEntryList(groupIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(usersDn)) {

            List<Entry> mergedEntries = Utils.nullableOneElementList(createSystemEntry(usersDn));

            if (!filterProcessor.match(context.getFilter(), Utils.OU_USERS, EntryType.UNIT))
                mergedEntries.remove(0);

            mergedEntries.addAll(createUserEntryList(findUsers(), context.getFilter()));

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(usersDn)) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(rootDn)) {

            List<Entry> mergedEntries = new ArrayList<>();

            if (filterProcessor.match(context.getFilter(), getId(), EntryType.DOMAIN))
                mergedEntries.add(createSystemEntry(rootDn));

            if (filterProcessor.match(context.getFilter(), Utils.OU_GROUPS, EntryType.UNIT))
                mergedEntries.add(createSystemEntry(groupsDn));

            if (filterProcessor.match(context.getFilter(), Utils.OU_USERS, EntryType.UNIT))
                mergedEntries.add(createSystemEntry(usersDn));

            mergedEntries.addAll(createGroupEntryList(findGroups(), context.getFilter()));
            mergedEntries.addAll(createUserEntryList(findUsers(), context.getFilter()));

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(mergedEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(rootDn)) {

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

        // will only search at one level
        return findManyOnFirstLevel(context);
    }
}
