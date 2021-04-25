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

import com.aservo.ldap.adapter.adapter.FilterMatcher;
import com.aservo.ldap.adapter.adapter.LdapUtils;
import com.aservo.ldap.adapter.adapter.entity.*;
import com.aservo.ldap.adapter.adapter.query.AndLogicExpression;
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import com.aservo.ldap.adapter.backend.DirectoryBackendFactory;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
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


/**
 * An ApacheDS partition that bridges to directory backends.
 */
public class CommonPartition
        extends SimpleReadOnlyPartition {

    private final Logger logger = LoggerFactory.getLogger(CommonPartition.class);

    private final DirectoryBackendFactory directoryFactory;
    private final ServerConfiguration serverConfig;
    private DomainEntity domainEntity;
    private GroupUnitEntity groupUnitEntity;
    private UserUnitEntity userUnitEntity;

    /**
     * Instantiates a new partition based on directory backend implementation.
     *
     * @param directoryFactory the directory backend factory
     * @param serverConfig     the server config
     */
    public CommonPartition(ServerConfiguration serverConfig, DirectoryBackendFactory directoryFactory) {

        super(directoryFactory.getPermanentDirectory().getId());

        this.serverConfig = serverConfig;
        this.directoryFactory = directoryFactory;
    }

    @Override
    protected void doInit()
            throws LdapException {

        domainEntity =
                new DomainEntity(directoryFactory.getPermanentDirectory().getId(),
                        serverConfig.getBaseDnDescription());

        groupUnitEntity = new GroupUnitEntity(serverConfig.getBaseDnGroupsDescription());
        userUnitEntity = new UserUnitEntity(serverConfig.getBaseDnUsersDescription());
    }

    @Override
    protected void doDestroy()
            throws LdapException {
    }

    @Override
    public Dn getSuffixDn() {

        return LdapUtils.createDn(schemaManager, directoryFactory.getPermanentDirectory(), EntityType.DOMAIN);
    }

    private FilterMatcher newFilterMatcher(DirectoryBackend directory) {

        return new FilterMatcher() {

            protected boolean evaluateUndefinedFilterExprSuccessfully() {

                return serverConfig.evaluateUndefinedFilterExprSuccessfully();
            }

            protected boolean isGroupMember(GroupEntity entity, String dn, boolean negated) {

                return LdapUtils.isGroupMember(schemaManager, directory, entity, dn,
                        serverConfig.isFlatteningEnabled(), negated);
            }

            protected boolean isMemberOfGroup(GroupEntity entity, String dn, boolean negated) {

                return LdapUtils.isMemberOfGroup(schemaManager, directory, entity, dn,
                        serverConfig.isFlatteningEnabled(), negated);
            }

            protected boolean isMemberOfGroup(UserEntity entity, String dn, boolean negated) {

                return LdapUtils.isMemberOfGroup(schemaManager, directory, entity, dn,
                        serverConfig.isFlatteningEnabled(), negated);
            }
        };
    }

    private Entry createEntry(DirectoryBackend directory, Entity entity, Set<String> attributes) {

        Dn queryDn = LdapUtils.createDn(schemaManager, directory, entity);
        Entry entry;

        if (entity instanceof DomainEntity) {

            // create root entry
            // dn: dc=<domain>
            // objectclass: top
            // objectclass: domain
            // description: <id> Domain

            DomainEntity domain = (DomainEntity) entity;
            entry = new DefaultEntry(schemaManager, queryDn);

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                entry.put(SchemaConstants.OBJECT_CLASS_AT,
                        SchemaConstants.TOP_OC,
                        SchemaConstants.DOMAIN_OC);
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.DC_AT)) {

                entry.put(SchemaConstants.DC_AT, domain.getId());
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                entry.put(SchemaConstants.DESCRIPTION_AT, domain.getDescription());
            }

        } else if (entity instanceof GroupUnitEntity) {

            // create groups entry
            // dn: ou=groups, dc=<domain>
            // objectClass: top
            // objectClass: organizationalUnit
            // ou: groups
            // description: <id> Groups

            GroupUnitEntity unit = (GroupUnitEntity) entity;
            entry = new DefaultEntry(schemaManager, queryDn);

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                entry.put(SchemaConstants.OBJECT_CLASS_AT,
                        SchemaConstants.TOP_OC,
                        SchemaConstants.ORGANIZATIONAL_UNIT_OC);
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                entry.put(SchemaConstants.OU_AT, LdapUtils.OU_GROUPS);
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                entry.put(SchemaConstants.DESCRIPTION_AT, unit.getDescription());
            }

        } else if (entity instanceof UserUnitEntity) {

            // create users entry
            // dn: ou=users, dc=<domain>
            // objectClass: top
            // objectClass: organizationalUnit
            // ou: users
            // description: <id> Users

            UserUnitEntity unit = (UserUnitEntity) entity;
            entry = new DefaultEntry(schemaManager, queryDn);

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                entry.put(SchemaConstants.OBJECT_CLASS_AT,
                        SchemaConstants.TOP_OC,
                        SchemaConstants.ORGANIZATIONAL_UNIT_OC);
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                entry.put(SchemaConstants.OU_AT, LdapUtils.OU_USERS);
            }

            if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                entry.put(SchemaConstants.DESCRIPTION_AT, unit.getDescription());
            }

        } else if (entity instanceof GroupEntity) {

            try {

                GroupEntity group = (GroupEntity) entity;
                entry = new DefaultEntry(schemaManager, queryDn);

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                    entry.put(SchemaConstants.OBJECT_CLASS_AT,
                            SchemaConstants.TOP_OC,
                            SchemaConstants.GROUP_OF_NAMES_OC,
                            SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                    entry.put(SchemaConstants.OU_AT, LdapUtils.OU_GROUPS);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.CN_AT_OID)) {

                    entry.put(SchemaConstants.CN_AT, group.getId());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    if (group.getDescription() != null && !group.getDescription().isEmpty())
                        entry.put(SchemaConstants.DESCRIPTION_AT, group.getDescription());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.MEMBER_AT_OID)) {

                    for (String memberDn : collectMemberDn(directory, group.getId()))
                        entry.add(SchemaConstants.MEMBER_AT, memberDn);
                }

                if (attributes.isEmpty() || attributes.contains(LdapUtils.MEMBER_OF_AT_OID)) {

                    for (String memberOfDn : collectMemberOfDnForGroup(directory, group.getId()))
                        entry.add(LdapUtils.MEMBER_OF_AT, memberOfDn);
                }

            } catch (EntityNotFoundException e) {

                logger.debug("Could not create group entry because some related entities are no longer available.", e);
                return null;

            } catch (LdapException e) {

                throw new RuntimeException(e);
            }

        } else if (entity instanceof UserEntity) {

            try {

                UserEntity user = (UserEntity) entity;
                entry = new DefaultEntry(schemaManager, queryDn);

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                    entry.put(SchemaConstants.OBJECT_CLASS_AT,
                            SchemaConstants.TOP_OC,
                            SchemaConstants.PERSON_OC,
                            SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                            SchemaConstants.INET_ORG_PERSON_OC);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                    entry.put(SchemaConstants.OU_AT, LdapUtils.OU_USERS);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.UID_AT_OID)) {

                    entry.put(SchemaConstants.UID_AT, user.getId());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.CN_AT_OID)) {

                    entry.put(SchemaConstants.CN_AT, user.getId());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.SN_AT_OID)) {

                    if (user.getLastName() != null && !user.getLastName().isEmpty()) {

                        if (serverConfig.isAbbreviateSnAttribute())
                            entry.put(SchemaConstants.SN_AT, user.getLastName());
                        else
                            entry.put(SchemaConstants.SURNAME_AT, user.getLastName());
                    }
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.GN_AT_OID)) {

                    if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {

                        if (serverConfig.isAbbreviateGnAttribute())
                            entry.put(SchemaConstants.GN_AT, user.getLastName());
                        else
                            entry.put(SchemaConstants.GIVENNAME_AT, user.getFirstName());
                    }
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DISPLAY_NAME_AT_OID)) {

                    if (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                        entry.put(SchemaConstants.DISPLAY_NAME_AT, user.getDisplayName());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.MAIL_AT_OID)) {

                    entry.put(SchemaConstants.MAIL_AT, user.getEmail());
                }

                if (attributes.isEmpty() || attributes.contains(LdapUtils.MEMBER_OF_AT_OID)) {

                    for (String memberOfDn : collectMemberOfDnForUser(directory, user.getId()))
                        entry.add(LdapUtils.MEMBER_OF_AT, memberOfDn);
                }

            } catch (EntityNotFoundException e) {

                logger.debug("Could not create user entry because some related entities are no longer available.", e);
                return null;

            } catch (LdapException e) {

                throw new RuntimeException(e);
            }

        } else
            throw new IllegalArgumentException("Cannot create system entry for DN=" + queryDn);

        return entry;
    }

    private List<String> collectMemberDn(DirectoryBackend directory, String groupId)
            throws EntityNotFoundException {

        return Stream.concat(
                LdapUtils.findGroupMembers(directory, groupId, serverConfig.isFlatteningEnabled()).stream()
                        .map(x -> LdapUtils.createDn(schemaManager, directory, EntityType.GROUP, x)),
                LdapUtils.findUserMembers(directory, groupId, serverConfig.isFlatteningEnabled()).stream()
                        .map(x -> LdapUtils.createDn(schemaManager, directory, EntityType.USER, x))
        )
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    private List<String> collectMemberOfDnForGroup(DirectoryBackend directory, String groupId)
            throws EntityNotFoundException {

        return LdapUtils.findGroupMembersReverse(directory, groupId, serverConfig.isFlatteningEnabled()).stream()
                .map(x -> LdapUtils.createDn(schemaManager, directory, EntityType.GROUP, x))
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    private List<String> collectMemberOfDnForUser(DirectoryBackend directory, String userId)
            throws EntityNotFoundException {

        return LdapUtils.findUserMembersReverse(directory, userId, serverConfig.isFlatteningEnabled()).stream()
                .map(x -> LdapUtils.createDn(schemaManager, directory, EntityType.GROUP, x))
                .map(Dn::getName)
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public ClonedServerEntry lookup(LookupOperationContext context)
            throws LdapException {

        logger.debug("Lookup cache for entry with DN={}",
                context.getDn().getName());

        return directoryFactory.withSession(directory -> {

            Dn rootDn = LdapUtils.createDn(schemaManager, directory, EntityType.DOMAIN);
            Dn groupsDn = LdapUtils.createDn(schemaManager, directory, EntityType.GROUP_UNIT);
            Dn usersDn = LdapUtils.createDn(schemaManager, directory, EntityType.USER_UNIT);

            Entry entry = null;

            if (context.getDn().equals(groupsDn)) {

                entry = createEntry(directory, groupUnitEntity, Collections.emptySet());

            } else if (context.getDn().getParent().equals(groupsDn)) {

                String groupId = LdapUtils.getGroupIdFromDn(schemaManager, directory, context.getDn().getName());

                if (groupId != null && directory.isKnownGroup(groupId)) {

                    try {

                        entry = createEntry(directory, directory.getGroup(groupId), Collections.emptySet());

                    } catch (EntityNotFoundException e) {
                    }
                }

            } else if (context.getDn().equals(usersDn)) {

                entry = createEntry(directory, userUnitEntity, Collections.emptySet());

            } else if (context.getDn().getParent().equals(usersDn)) {

                String userId = LdapUtils.getUserIdFromDn(schemaManager, directory, context.getDn().getName());

                if (userId != null && directory.isKnownUser(userId)) {

                    try {

                        entry = createEntry(directory, directory.getUser(userId), Collections.emptySet());

                    } catch (EntityNotFoundException e) {
                    }
                }

            } else if (context.getDn().equals(rootDn)) {

                entry = createEntry(directory, domainEntity, Collections.emptySet());
            }

            if (entry == null) {

                logger.debug("Could not find cached entry with DN={}", context.getDn().getName());
                return null;

            } else {

                logger.debug("Could find cached entry with DN={}", context.getDn().getName());
                return new ClonedServerEntry(entry);
            }
        });
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        logger.debug("Check for existence of entry with DN={}",
                context.getDn().getName());

        return directoryFactory.withSession(directory -> {

            Dn rootDn = LdapUtils.createDn(schemaManager, directory, EntityType.DOMAIN);
            Dn groupsDn = LdapUtils.createDn(schemaManager, directory, EntityType.GROUP_UNIT);
            Dn usersDn = LdapUtils.createDn(schemaManager, directory, EntityType.USER_UNIT);
            FilterMatcher filterMatcher = newFilterMatcher(directory);

            if (context.getDn().equals(groupsDn)) {

                return true;

            } else if (context.getDn().getParent().equals(groupsDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();
                FilterNode filterNode = new EqualOperator(attribute, value);

                return directory.getGroups(filterNode, Optional.of(filterMatcher)).stream().findAny().isPresent();

            } else if (context.getDn().equals(usersDn)) {

                return true;

            } else if (context.getDn().getParent().equals(usersDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();
                FilterNode filterNode = new EqualOperator(attribute, value);

                return directory.getUsers(filterNode, Optional.of(filterMatcher)).stream().findAny().isPresent();

            } else if (context.getDn().equals(rootDn)) {

                return true;

            } else if (context.getDn().getParent().equals(rootDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();
                FilterNode filterNode = new EqualOperator(attribute, value);

                return directory.getGroups(filterNode, Optional.of(filterMatcher)).stream().findAny().isPresent() ||
                        directory.getUsers(filterNode, Optional.of(filterMatcher)).stream().findAny().isPresent();
            }

            return false;
        });
    }

    private List<Entry> findEntries(SearchOperationContext context, boolean ouOnly) {

        return directoryFactory.withSession(directory -> {

            Dn rootDn = LdapUtils.createDn(schemaManager, directory, EntityType.DOMAIN);
            Dn groupsDn = LdapUtils.createDn(schemaManager, directory, EntityType.GROUP_UNIT);
            Dn usersDn = LdapUtils.createDn(schemaManager, directory, EntityType.USER_UNIT);
            FilterMatcher filterMatcher = newFilterMatcher(directory);

            Set<String> attributes =
                    Arrays.stream(context.getReturningAttributesString())
                            .map(LdapUtils::normalizeAttribute)
                            .collect(Collectors.toSet());

            FilterNode filterNode = LdapUtils.createInternalFilterNode(context.getFilter());
            List<Entity> mergedEntities = new ArrayList<>();

            if (context.getDn().equals(groupsDn)) {

                if (filterMatcher.matchEntity(groupUnitEntity, filterNode))
                    mergedEntities.add(groupUnitEntity);

                if (!ouOnly) {

                    mergedEntities.addAll(directory.getGroups(filterNode, Optional.of(filterMatcher)));
                }

            } else if (context.getDn().getParent().equals(groupsDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();

                filterNode = new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), filterNode));

                mergedEntities.addAll(directory.getGroups(filterNode, Optional.of(filterMatcher)));

            } else if (context.getDn().equals(usersDn)) {

                if (filterMatcher.matchEntity(userUnitEntity, filterNode))
                    mergedEntities.add(userUnitEntity);

                if (!ouOnly) {

                    mergedEntities.addAll(directory.getUsers(filterNode, Optional.of(filterMatcher)));
                }

            } else if (context.getDn().getParent().equals(usersDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();

                filterNode = new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), filterNode));

                mergedEntities.addAll(directory.getUsers(filterNode, Optional.of(filterMatcher)));

            } else if (context.getDn().equals(rootDn)) {

                if (filterMatcher.matchEntity(domainEntity, filterNode))
                    mergedEntities.add(domainEntity);

                if (!ouOnly) {

                    if (filterMatcher.matchEntity(groupUnitEntity, filterNode))
                        mergedEntities.add(groupUnitEntity);

                    if (filterMatcher.matchEntity(userUnitEntity, filterNode))
                        mergedEntities.add(userUnitEntity);

                    mergedEntities.addAll(directory.getGroups(filterNode, Optional.of(filterMatcher)));
                    mergedEntities.addAll(directory.getUsers(filterNode, Optional.of(filterMatcher)));
                }

            } else if (context.getDn().getParent().equals(rootDn)) {

                String attribute = context.getDn().getRdn().getType();
                String value = context.getDn().getRdn().getNormValue();

                filterNode = new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), filterNode));

                mergedEntities.addAll(directory.getGroups(filterNode, Optional.of(filterMatcher)));
                mergedEntities.addAll(directory.getUsers(filterNode, Optional.of(filterMatcher)));
            }

            return mergedEntities.stream().map(x -> createEntry(directory, x, attributes)).collect(Collectors.toList());
        });
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context)
            throws LdapException {

        return findEntries(context, true).stream().findAny()
                .map(entry -> new EntryFilteringCursorImpl(new SingletonCursor<>(entry), context, schemaManager))
                .orElse(new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager));
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        return new EntryFilteringCursorImpl(new ListCursor<>(findEntries(context, false)), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        // will only search at one level
        return findManyOnFirstLevel(context);
    }
}
