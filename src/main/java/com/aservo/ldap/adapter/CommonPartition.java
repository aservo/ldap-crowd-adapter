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

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.cursor.ClosableIterator;
import com.aservo.ldap.adapter.api.cursor.ClosableIteratorJoin;
import com.aservo.ldap.adapter.api.cursor.IterableEntryCursor;
import com.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.api.entity.*;
import com.aservo.ldap.adapter.api.query.AndLogicExpression;
import com.aservo.ldap.adapter.api.query.BooleanValue;
import com.aservo.ldap.adapter.api.query.EqualOperator;
import com.aservo.ldap.adapter.api.query.QueryExpression;
import java.io.IOException;
import java.util.*;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
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

        return LdapUtils.createDn(schemaManager, EntityType.DOMAIN, getId());
    }

    @Nullable
    @Override
    public ClonedServerEntry lookup(LookupOperationContext context)
            throws LdapException {

        logger.debug("[Thread ID {}] - Perform lookup operation for entry with DN={}",
                Thread.currentThread().getId(), context.getDn().getName());

        QueryExpression expression = BooleanValue.trueValue();
        Set<String> attributes = LdapUtils.getAttributes(context);
        ClosableIterator<Entry> entries = findEntries(expression, context.getDn(), attributes, false);

        if (!entries.hasNext()) {

            entries.closeUnchecked();
            logger.debug("Could not find cached entry with DN={}", context.getDn().getName());

            return null;
        }

        Entry entry = entries.next();

        entries.closeUnchecked();
        logger.debug("Could find cached entry with DN={}", context.getDn().getName());

        return new ClonedServerEntry(entry);
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        logger.debug("[Thread ID {}] - Perform check for existence of entry with DN={}",
                Thread.currentThread().getId(), context.getDn().getName());

        QueryExpression expression = BooleanValue.trueValue();
        Set<String> attributes = Collections.emptySet();
        ClosableIterator<Entry> entries = findEntries(expression, context.getDn(), attributes, false);

        boolean exists = entries.hasNext();

        entries.closeUnchecked();

        return exists;
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context)
            throws LdapException {

        logger.debug("[Thread ID {}] - Perform search for a single entry with DN={}",
                Thread.currentThread().getId(), context.getDn().getName());

        QueryExpression expression = LdapUtils.createQueryExpression(context.getFilter());
        Set<String> attributes = LdapUtils.getAttributes(context);
        ClosableIterator<Entry> entries = findEntries(expression, context.getDn(), attributes, false);

        if (!entries.hasNext()) {

            entries.closeUnchecked();

            return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
        }

        Entry entry = entries.next();

        entries.closeUnchecked();

        return new EntryFilteringCursorImpl(new SingletonCursor<>(entry), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        logger.debug("[Thread ID {}] - Perform search for entries with DN={}",
                Thread.currentThread().getId(), context.getDn().getName());

        QueryExpression expression = LdapUtils.createQueryExpression(context.getFilter());
        Set<String> attributes = LdapUtils.getAttributes(context);
        ClosableIterator<Entry> entries = findEntries(expression, context.getDn(), attributes, true);

        return new EntryFilteringCursorImpl(new IterableEntryCursor(entries), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        // will only search at one level
        return findManyOnFirstLevel(context);
    }

    private ClosableIterator<Entry> findEntries(QueryExpression expression, Dn queryDn, Set<String> attributes,
                                                boolean multiple) {

        Dn rootDn = LdapUtils.createDn(schemaManager, EntityType.DOMAIN, getId());
        Dn groupsDn = LdapUtils.createDn(schemaManager, EntityType.GROUP_UNIT, getId());
        Dn usersDn = LdapUtils.createDn(schemaManager, EntityType.USER_UNIT, getId());

        return directoryFactory.withSession(directory -> {

            List<Iterator<Entity>> iterators = new ArrayList<>();

            if (queryDn.equals(groupsDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, groupUnitEntity)))
                    iterators.add(Collections.singleton((Entity) groupUnitEntity).iterator());

                if (multiple) {

                    iterators.add(directory.runQueryExpression(schemaManager, expression, EntityType.GROUP));
                }

            } else if (queryDn.getParent().equals(groupsDn)) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getNormValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                iterators.add(directory.runQueryExpression(schemaManager, expr, EntityType.GROUP));

            } else if (queryDn.equals(usersDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, userUnitEntity)))
                    iterators.add(Collections.singleton((Entity) userUnitEntity).iterator());

                if (multiple) {

                    iterators.add(directory.runQueryExpression(schemaManager, expression, EntityType.USER));
                }

            } else if (queryDn.getParent().equals(usersDn)) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getNormValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                iterators.add(directory.runQueryExpression(schemaManager, expr, EntityType.USER));

            } else if (queryDn.equals(rootDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, domainEntity)))
                    iterators.add(Collections.singleton((Entity) domainEntity).iterator());

                if (multiple) {

                    if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, groupUnitEntity)))
                        iterators.add(Collections.singleton((Entity) groupUnitEntity).iterator());

                    if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, userUnitEntity)))
                        iterators.add(Collections.singleton((Entity) userUnitEntity).iterator());

                    iterators.add(directory.runQueryExpression(schemaManager, expression, EntityType.GROUP));
                    iterators.add(directory.runQueryExpression(schemaManager, expression, EntityType.USER));
                }

            } else if (queryDn.getParent().equals(rootDn) && multiple) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getNormValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                iterators.add(directory.runQueryExpression(schemaManager, expr, EntityType.GROUP));
                iterators.add(directory.runQueryExpression(schemaManager, expr, EntityType.USER));
            }

            return createEntries(new ClosableIteratorJoin<>(iterators), attributes);
        });
    }

    private Entry createEntry(Entity entity, Set<String> attributes) {

        Dn queryDn = LdapUtils.createDn(schemaManager, entity, getId());
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

                    entry.put(SchemaConstants.CN_AT, group.getName());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    if (group.getDescription() != null && !group.getDescription().isEmpty())
                        entry.put(SchemaConstants.DESCRIPTION_AT, group.getDescription());
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.MEMBER_AT_OID)) {

                    if (!serverConfig.isFlatteningEnabled()) {

                        for (String name : group.getMemberNamesGroup())
                            entry.add(SchemaConstants.MEMBER_AT,
                                    LdapUtils.createDn(schemaManager, EntityType.GROUP, name, getId()).getName());
                    }

                    for (String name : group.getMemberNamesUser())
                        entry.add(SchemaConstants.MEMBER_AT,
                                LdapUtils.createDn(schemaManager, EntityType.USER, name, getId()).getName());
                }

                if (attributes.isEmpty() || attributes.contains(LdapUtils.MEMBER_OF_AT_OID)) {

                    if (!serverConfig.isFlatteningEnabled()) {

                        for (String name : group.getMemberOfNames())
                            entry.add(LdapUtils.MEMBER_OF_AT,
                                    LdapUtils.createDn(schemaManager, EntityType.GROUP, name, getId()).getName());
                    }
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

                    entry.put(SchemaConstants.CN_AT, user.getUsername());
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
                            entry.put(SchemaConstants.GN_AT, user.getFirstName());
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

                    for (String name : user.getMemberOfNames())
                        entry.add(LdapUtils.MEMBER_OF_AT,
                                LdapUtils.createDn(schemaManager, EntityType.GROUP, name, getId()).getName());
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

    private ClosableIterator<Entry> createEntries(ClosableIterator<Entity> entities, Set<String> attributes) {

        return new ClosableIterator<Entry>() {

            public boolean hasNext() {

                return entities.hasNext();
            }

            public Entry next() {

                return createEntry(entities.next(), attributes);
            }

            public void close()
                    throws IOException {

                entities.close();
            }
        };
    }
}
