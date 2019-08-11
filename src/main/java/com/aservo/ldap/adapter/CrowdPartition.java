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

import com.aservo.crowd.ldap.util.*;
import com.aservo.ldap.adapter.util.*;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.GroupWithAttributes;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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


public class CrowdPartition
        extends SimpleReadOnlyPartition {

    private final Logger logger = LoggerFactory.getLogger(CrowdPartition.class);

    private final CrowdClient crowdClient;
    private final ServerConfiguration serverConfig;
    private LRUCacheMap<String, Entry> entryCache;
    private FilterMatcher filterProcessor;

    private Entry crowdEntry;
    private Entry crowdGroupsEntry;
    private Entry crowdUsersEntry;

    public CrowdPartition(CrowdClient crowdClient, ServerConfiguration serverConfig) {

        super("crowd");

        this.crowdClient = crowdClient;
        this.serverConfig = serverConfig;
        this.entryCache = new LRUCacheMap<>(serverConfig.getEntryCacheMaxSize());
    }

    @Override
    protected void doInit()
            throws Exception {

        // create crowd entry
        // dn: dc=crowd
        // objectclass: top
        // objectclass: domain
        // description: Crowd Domain
        Dn crowdDn = new Dn(schemaManager, Utils.CROWD_DN);
        crowdEntry = new DefaultEntry(schemaManager, crowdDn);
        crowdEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.DOMAIN_OC);
        crowdEntry.put(SchemaConstants.DC_AT, crowdDn.getRdn().getAva().getValue().toString());
        crowdEntry.put(SchemaConstants.DESCRIPTION_AT, "Crowd Domain");

        // create groups entry
        // dn: ou=groups, dc=crowd
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: groups
        // description: Crowd Groups
        Dn groupDn = new Dn(schemaManager, Utils.CROWD_GROUPS_DN);
        crowdGroupsEntry = new DefaultEntry(schemaManager, groupDn);
        crowdGroupsEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        crowdGroupsEntry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
        crowdGroupsEntry.put(SchemaConstants.DESCRIPTION_AT, "Crowd Groups");

        // create users entry
        // dn: ou=users, dc=crowd
        // objectClass: top
        // objectClass: organizationalUnit
        // ou: users
        // description: Crowd Users
        Dn usersDn = new Dn(schemaManager, Utils.CROWD_USERS_DN);
        crowdUsersEntry = new DefaultEntry(schemaManager, usersDn);
        crowdUsersEntry.put(SchemaConstants.OBJECT_CLASS_AT,
                SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC);
        crowdUsersEntry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
        crowdUsersEntry.put(SchemaConstants.DESCRIPTION_AT, "Crowd Users");

        // add to cache
        if (serverConfig.isEntryCacheEnabled()) {

            entryCache.put(crowdDn.getName(), crowdEntry);
            entryCache.put(groupDn.getName(), crowdGroupsEntry);
            entryCache.put(usersDn.getName(), crowdUsersEntry);
        }

        filterProcessor =
                new FilterMatcher() {

                    @Nullable
                    @Override
                    protected String getAttributeValue(String attribute, String entryId, OuType ouType) {

                        try {

                            if (ouType.equals(OuType.USER)) {

                                try {

                                    String userName = crowdClient.getUser(entryId).getName();
                                    UserWithAttributes attributes = crowdClient.getUserWithAttributes(userName);

                                    return attributes.getValue(attribute);

                                } catch (UserNotFoundException e) {
                                }

                            } else if (ouType.equals(OuType.GROUP)) {

                                try {

                                    String groupName = crowdClient.getGroup(entryId).getName();
                                    GroupWithAttributes attributes = crowdClient.getGroupWithAttributes(groupName);

                                    return attributes.getValue(attribute);

                                } catch (GroupNotFoundException e) {
                                }
                            }

                        } catch (OperationFailedException |
                                ApplicationPermissionException |
                                InvalidAuthenticationException e) {

                            logger.error("Cannot get value of user attribute.");
                        }

                        return null;
                    }
                };
    }

    @Override
    protected void doDestroy()
            throws Exception {

        logger.info("destroying partition");
        crowdClient.shutdown();
    }

    @Override
    public Dn getSuffixDn() {

        return crowdEntry.getDn();
    }

    @Nullable
    private Entry createGroupEntry(Dn dn) {

        Entry entry = entryCache.get(dn.getName());

        if (entry != null)
            return entry;

        try {

            String groupId = dn.getRdn().getNormValue();
            Group group = crowdClient.getGroup(groupId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_GROUPS);
            entry.put(SchemaConstants.CN_AT, group.getName());
            entry.put(SchemaConstants.DESCRIPTION_AT, group.getDescription());

            List<Dn> userDns = findMembers(groupId).stream()
                    .map(this::createUserDn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (Dn userDn : userDns)
                entry.add(SchemaConstants.MEMBER_AT, userDn.getName());

            // add to cache
            if (serverConfig.isEntryCacheEnabled())
                entryCache.put(dn.getName(), entry);

        } catch (GroupNotFoundException |
                ApplicationPermissionException |
                InvalidAuthenticationException |
                OperationFailedException e) {

            logger.debug("Could not create group entry because of problems with Crowd request.", e);
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
            User user = crowdClient.getUser(userId);

            entry = new DefaultEntry(schemaManager, dn);

            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ORGANIZATIONAL_PERSON_OC);
            entry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.INET_ORG_PERSON_OC);
            entry.put(SchemaConstants.OU_AT, Utils.OU_USERS);
            entry.put(SchemaConstants.UID_AT, user.getName());
            entry.put(SchemaConstants.CN_AT, user.getName());
            entry.put(SchemaConstants.COMMON_NAME_AT, user.getName());
            entry.put(SchemaConstants.GN_AT, user.getFirstName());
            entry.put(SchemaConstants.GIVENNAME_AT, user.getFirstName());
            entry.put(SchemaConstants.SN_AT, user.getLastName());
            entry.put(SchemaConstants.SURNAME_AT, user.getLastName());
            entry.put(SchemaConstants.DISPLAY_NAME_AT, user.getDisplayName());
            entry.put(SchemaConstants.MAIL_AT, user.getEmailAddress());
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

        } catch (UserNotFoundException |
                ApplicationPermissionException |
                InvalidAuthenticationException |
                OperationFailedException e) {

            logger.debug("Could not create user entry because of problems with Crowd request.", e);
            return null;

        } catch (LdapException e) {

            logger.debug("Could not create user entry because of an incorrect build.", e);
            return null;
        }

        return entry;
    }

    private List<String> findMembers(String groupId) {

        try {

            List<String> userIds = crowdClient.getNamesOfUsersOfGroup(groupId, 0, Integer.MAX_VALUE);

            if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING)) {

                // works transitive
                for (String y : crowdClient.getNamesOfNestedChildGroupsOfGroup(groupId, 0, Integer.MAX_VALUE))
                    for (String x : crowdClient.getNamesOfUsersOfGroup(y, 0, Integer.MAX_VALUE))
                        if (!userIds.contains(x))
                            userIds.add(x);
            }

            return userIds;

        } catch (GroupNotFoundException |
                ApplicationPermissionException |
                InvalidAuthenticationException |
                OperationFailedException e) {

            logger.debug("Could not collect group members because of problems with Crowd request.", e);
            return Collections.emptyList();
        }
    }

    private List<String> findGroupsForMemberOf(String userId) {

        try {

            List<String> groupIds = new LinkedList<>();

            if (serverConfig.getMemberOfSupport().allowMemberOfAttribute()) {

                groupIds.addAll(crowdClient.getNamesOfGroupsForUser(userId, 0, Integer.MAX_VALUE));

                if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.NESTED_GROUPS)) {

                    for (String x : crowdClient.getNamesOfGroupsForNestedUser(userId, 0, Integer.MAX_VALUE))
                        if (!groupIds.contains(x))
                            groupIds.add(x);

                } else if (serverConfig.getMemberOfSupport().equals(MemberOfSupport.FLATTENING)) {

                    List<String> flatGroupList = new LinkedList<>(groupIds);

                    for (String y : flatGroupList) {

                        // works transitive
                        List<String> result = crowdClient.getNamesOfParentGroupsForNestedGroup(y, 0, Integer.MAX_VALUE);

                        for (String x : result)
                            if (!groupIds.contains(x))
                                groupIds.add(x);
                    }
                }
            }

            return groupIds;

        } catch (UserNotFoundException |
                GroupNotFoundException |
                ApplicationPermissionException |
                InvalidAuthenticationException |
                OperationFailedException e) {

            logger.debug("Could not collect groups for a member because of problems with Crowd request.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ClonedServerEntry lookup(LookupOperationContext context) {

        if (!serverConfig.isEntryCacheEnabled())
            return null;

        Dn dn = context.getDn();
        Entry se = entryCache.get(context.getDn().getName());
        if (se == null) {
            //todo
            logger.debug("lookup()::No cached entry found for " + dn.getName());
            return null;
        } else {
            logger.debug("lookup()::Cached entry found for " + dn.getName());
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
            if (crowdEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdEntry);
                return true;
            }

            return false;
        }

        // two levels in DN
        if (dnSize == 2) {
            if (crowdGroupsEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdGroupsEntry);
                return true;
            }
            if (crowdUsersEntry.getDn().equals(dn)) {
                entryCache.put(dn.getName(), crowdUsersEntry);
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

            if (crowdUsersEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String user = rdn.getNormValue();
                logger.debug("user={}", user);
                Entry userEntry = createUserEntry(dn);
                return (userEntry != null);
            }

            if (crowdGroupsEntry.getDn().equals(prefix)) {
                Rdn rdn = dn.getRdn(2);
                String group = rdn.getNormValue();
                logger.debug("group={}", group);
                Entry groupEntry = createGroupEntry(dn);
                return (groupEntry != null);
            }

            logger.debug("Prefix is neither users nor groups");
            logger.debug("Crowd Users = {}", crowdUsersEntry.getDn());
            logger.debug("Crowd Groups = {}", crowdGroupsEntry.getDn().toString());
            return false;
        }

        return false;
    }

    @Nullable
    private Dn createGroupDn(String groupId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", groupId, Utils.CROWD_GROUPS_DN));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create group DN.", e);
            return null;
        }
    }

    @Nullable
    private Dn createUserDn(String userId) {

        try {

            return new Dn(schemaManager, String.format("cn=%s,%s", userId, Utils.CROWD_USERS_DN));

        } catch (LdapInvalidDnException e) {

            logger.error("Cannot create user DN.", e);
            return null;
        }
    }

    private List<String> findGroups() {

        try {

            return crowdClient.searchGroupNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.debug("Cannot receive group information from Crowd.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findGroup(String attribute, String value) {

        SearchRestriction restriction;
        List<String> result;

        try {

            switch (attribute) {

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    restriction = new TermRestriction<>(GroupTermKeys.NAME, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchGroupNames(restriction, 0, Integer.MAX_VALUE);

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

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return null;
        }
    }

    private List<String> findUsers() {

        try {

            return crowdClient.searchUserNames(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return Collections.emptyList();
        }
    }

    @Nullable
    private String findUser(String attribute, String value) {

        SearchRestriction restriction;
        List<String> result;

        try {

            switch (attribute) {

                case SchemaConstants.UID_NUMBER_AT:
                case SchemaConstants.UID_NUMBER_AT_OID:

                    restriction = NullRestrictionImpl.INSTANCE;

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE).stream()
                            .filter((x) -> Utils.calculateHash(x).toString().equals(value))
                            .collect(Collectors.toList());

                    break;

                case SchemaConstants.UID_AT:
                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT:
                case SchemaConstants.CN_AT_OID:
                case SchemaConstants.COMMON_NAME_AT:

                    restriction = new TermRestriction<>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

                    break;

                case SchemaConstants.MAIL_AT:
                case SchemaConstants.MAIL_AT_OID:

                    restriction = new TermRestriction<>(UserTermKeys.EMAIL, MatchMode.EXACTLY_MATCHES, value);

                    result = crowdClient.searchUserNames(restriction, 0, Integer.MAX_VALUE);

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

        } catch (OperationFailedException |
                InvalidAuthenticationException |
                ApplicationPermissionException e) {

            logger.error("Cannot receive user information from Crowd.", e);

            return null;
        }
    }

    private List<Entry> createGroupEntryList(List<String> groupIds, ExprNode filter) {

        return groupIds.stream()
                .filter((x) -> filterProcessor.match(filter, x, OuType.GROUP))
                .map(this::createGroupDn)
                .filter(Objects::nonNull)
                .map(this::createGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Entry> createUserEntryList(List<String> userIds, ExprNode filter) {

        return userIds.stream()
                .filter((x) -> filterProcessor.match(filter, x, OuType.USER))
                .map(this::createUserDn)
                .filter(Objects::nonNull)
                .map(this::createUserEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context) {

        logger.debug("findOne()::dn={}", context.getDn().getName());

        if (context.getDn().getParent().equals(crowdGroupsEntry.getDn())) {

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

        } else if (context.getDn().getParent().equals(crowdUsersEntry.getDn())) {

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

        } else if (context.getDn().getParent().equals(crowdEntry.getDn())) {

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

        logger.debug("findManyOnFirstLevel()::dn={}", context.getDn().getName());

        if (context.getDn().equals(crowdGroupsEntry.getDn())) {

            List<Entry> groupEntryList = createGroupEntryList(findGroups(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(crowdGroupsEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> groupIds = Utils.nullableSingletonList(findGroup(attribute, value));
            List<Entry> groupEntryList = createGroupEntryList(groupIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(groupEntryList),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(crowdUsersEntry.getDn())) {

            List<Entry> userEntries = createUserEntryList(findUsers(), context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().getParent().equals(crowdUsersEntry.getDn())) {

            String attribute = context.getDn().getRdn().getType();
            String value = context.getDn().getRdn().getNormValue();

            List<String> userIds = Utils.nullableSingletonList(findUser(attribute, value));
            List<Entry> userEntries = createUserEntryList(userIds, context.getFilter());

            return new EntryFilteringCursorImpl(
                    new ListCursor<>(userEntries),
                    context,
                    schemaManager
            );

        } else if (context.getDn().equals(crowdEntry.getDn())) {

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

        } else if (context.getDn().getParent().equals(crowdEntry.getDn())) {

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

        logger.debug("findManyOnMultipleLevels()::dn={}", context.getDn().getName());

        // will only search at one level
        return findManyOnFirstLevel(context);
    }
}
