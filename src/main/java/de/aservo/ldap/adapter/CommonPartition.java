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

package de.aservo.ldap.adapter;

import de.aservo.ldap.adapter.api.LdapUtils;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.cursor.apacheds.EntryFilteringWrapperCursor;
import de.aservo.ldap.adapter.api.cursor.apacheds.IterableEntryCursor;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.database.exception.UnknownColumnException;
import de.aservo.ldap.adapter.api.entity.*;
import de.aservo.ldap.adapter.api.query.AndLogicExpression;
import de.aservo.ldap.adapter.api.query.BooleanValue;
import de.aservo.ldap.adapter.api.query.EqualOperator;
import de.aservo.ldap.adapter.api.query.QueryExpression;
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
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


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

        this.serverConfig = serverConfig;
        this.directoryFactory = directoryFactory;

        setId(directoryFactory.getPermanentDirectory().getId());
    }

    @Override
    protected void doInit()
            throws LdapException {

        setSuffixDn(LdapUtils.createDn(schemaManager, EntityType.DOMAIN, getId()));

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
    public ClonedServerEntry lookup(LookupOperationContext context)
            throws LdapException {

        logger.info("[{}] - Perform lookup operation for entry with DN={}",
                context.getSession().getClientAddress(), context.getDn().getName());

        PartitionTxn transaction = context.getTransaction();
        QueryExpression expression = BooleanValue.trueValue();
        Set<String> attributes = LdapUtils.getAttributes(context);
        MappableCursor<Entry> entries = findEntries(expression, context.getDn(), attributes, transaction, false);

        if (!entries.next()) {

            entries.closeUnchecked();
            logger.debug("Could not find cached entry with DN={}", context.getDn().getName());

            return null;
        }

        Entry entry = entries.get();

        entries.closeUnchecked();
        logger.debug("Could find cached entry with DN={}", context.getDn().getName());

        return new ClonedServerEntry(entry);
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext context)
            throws LdapException {

        logger.info("[{}] - Perform check for existence of entry with DN={}",
                context.getSession().getClientAddress(), context.getDn().getName());

        PartitionTxn transaction = context.getTransaction();
        QueryExpression expression = BooleanValue.trueValue();
        Set<String> attributes = Collections.emptySet();
        MappableCursor<Entry> entries = findEntries(expression, context.getDn(), attributes, transaction, false);

        boolean exists = entries.next();

        entries.closeUnchecked();

        return exists;
    }

    @Override
    protected boolean compare(CompareOperationContext context)
            throws LdapException {

        logger.info("[{}] - Perform compare action with DN={} compare={}:{}",
                context.getSession().getClientAddress(), context.getDn().getName(),
                context.getOid(), context.getValue().getString());

        PartitionTxn transaction = context.getTransaction();
        QueryExpression expression = new EqualOperator(context.getOid(), context.getValue().getString());
        Set<String> attributes = Collections.emptySet();
        MappableCursor<Entry> entries = findEntries(expression, context.getDn(), attributes, transaction, true);

        boolean exists = entries.next();

        entries.closeUnchecked();

        return exists;
    }

    @Override
    protected EntryFilteringCursor findOne(SearchOperationContext context)
            throws LdapException {

        logger.debug("Perform search for a single entry with DN={}",
                context.getDn().getName());

        PartitionTxn transaction = context.getTransaction();
        QueryExpression expression = LdapUtils.createQueryExpression(context.getFilter());
        Set<String> attributes = LdapUtils.getAttributes(context);
        MappableCursor<Entry> entries = findEntries(expression, context.getDn(), attributes, transaction, false);

        if (!entries.next()) {

            entries.closeUnchecked();

            return new EntryFilteringCursorImpl(new EmptyCursor<>(), context, schemaManager);
        }

        Entry entry = entries.get();

        entries.closeUnchecked();

        return new EntryFilteringCursorImpl(new SingletonCursor<>(entry), context, schemaManager);
    }

    @Override
    protected EntryFilteringCursor findManyOnFirstLevel(SearchOperationContext context)
            throws LdapException {

        logger.debug("Perform search for entries with DN={}",
                context.getDn().getName());

        PartitionTxn transaction = context.getTransaction();
        QueryExpression expression = LdapUtils.createQueryExpression(context.getFilter());
        Set<String> attributes = LdapUtils.getAttributes(context);
        MappableCursor<Entry> entries = findEntries(expression, context.getDn(), attributes, transaction, true);

        return new EntryFilteringWrapperCursor(new IterableEntryCursor(logger, entries), context);
    }

    @Override
    protected EntryFilteringCursor findManyOnMultipleLevels(SearchOperationContext context)
            throws LdapException {

        // will only search at one level
        return findManyOnFirstLevel(context);
    }

    private MappableCursor<Entry> findEntries(QueryExpression expression, Dn queryDn, Set<String> attributes,
                                              PartitionTxn transaction, boolean multiple) {

        Dn rootDn = LdapUtils.createDn(schemaManager, EntityType.DOMAIN, getId());
        Dn groupsDn = LdapUtils.createDn(schemaManager, EntityType.GROUP_UNIT, getId());
        Dn usersDn = LdapUtils.createDn(schemaManager, EntityType.USER_UNIT, getId());

        if (!(transaction instanceof SimpleReadOnlyPartition.ReadTransaction))
            throw new IllegalArgumentException("Cannot process unexpected transaction type");

        String txId = ((SimpleReadOnlyPartition.ReadTransaction) transaction).getId();

        return directoryFactory.withSession(directory -> {

            List<MappableCursor<Row>> cursors = new ArrayList<>();

            if (queryDn.equals(groupsDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, groupUnitEntity)))
                    cursors.add(MappableCursor.fromIterable(Collections.singleton(groupUnitEntity)));

                if (multiple) {

                    cursors.add(directory.runQueryExpression(txId, schemaManager, expression, EntityType.GROUP));
                }

            } else if (queryDn.getParent().equals(groupsDn)) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                cursors.add(directory.runQueryExpression(txId, schemaManager, expr, EntityType.GROUP));

            } else if (queryDn.equals(usersDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, userUnitEntity)))
                    cursors.add(MappableCursor.fromIterable(Collections.singleton(userUnitEntity)));

                if (multiple) {

                    cursors.add(directory.runQueryExpression(txId, schemaManager, expression, EntityType.USER));
                }

            } else if (queryDn.getParent().equals(usersDn)) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                cursors.add(directory.runQueryExpression(txId, schemaManager, expr, EntityType.USER));

            } else if (queryDn.equals(rootDn)) {

                if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, domainEntity)))
                    cursors.add(MappableCursor.fromIterable(Collections.singleton(domainEntity)));

                if (multiple) {

                    if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, groupUnitEntity)))
                        cursors.add(MappableCursor.fromIterable(Collections.singleton(groupUnitEntity)));

                    if (LdapUtils.evaluateExpression(LdapUtils.preEvaluateExpression(expression, userUnitEntity)))
                        cursors.add(MappableCursor.fromIterable(Collections.singleton(userUnitEntity)));

                    cursors.add(directory.runQueryExpression(txId, schemaManager, expression, EntityType.GROUP));
                    cursors.add(directory.runQueryExpression(txId, schemaManager, expression, EntityType.USER));
                }

            } else if (queryDn.getParent().equals(rootDn) && multiple) {

                String attribute = queryDn.getRdn().getType();
                String value = queryDn.getRdn().getValue();

                QueryExpression expr =
                        new AndLogicExpression(Arrays.asList(new EqualOperator(attribute, value), expression));

                cursors.add(directory.runQueryExpression(txId, schemaManager, expr, EntityType.GROUP));
                cursors.add(directory.runQueryExpression(txId, schemaManager, expr, EntityType.USER));
            }

            return createEntries(MappableCursor.flatten(cursors), attributes);
        });
    }

    private Entry createEntry(Row entity, Set<String> attributes) {

        EntityType entityType = EntityType.fromString(entity.apply(ColumnNames.TYPE, String.class));

        switch (entityType) {

            case DOMAIN: {

                // create root entry
                // dn: dc=<domain>
                // objectclass: top
                // objectclass: domain
                // description: <id> Domain

                String id = entity.apply(ColumnNames.ID, String.class);
                String description = entity.apply(ColumnNames.DESCRIPTION, String.class);
                Dn dn = LdapUtils.createDn(schemaManager, entityType, id, getId());
                Entry entry = new DefaultEntry(schemaManager, dn);

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                    entry.put(SchemaConstants.OBJECT_CLASS_AT,
                            SchemaConstants.TOP_OC,
                            SchemaConstants.DOMAIN_OC);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DC_AT)) {

                    entry.put(SchemaConstants.DC_AT, id);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    entry.put(SchemaConstants.DESCRIPTION_AT, description);
                }

                return entry;
            }

            case GROUP_UNIT: {

                // create groups entry
                // dn: ou=groups, dc=<domain>
                // objectClass: top
                // objectClass: organizationalUnit
                // ou: groups
                // description: <id> Groups

                String id = entity.apply(ColumnNames.ID, String.class);
                String description = entity.apply(ColumnNames.DESCRIPTION, String.class);
                Dn dn = LdapUtils.createDn(schemaManager, entityType, id, getId());
                Entry entry = new DefaultEntry(schemaManager, dn);

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                    entry.put(SchemaConstants.OBJECT_CLASS_AT,
                            SchemaConstants.TOP_OC,
                            SchemaConstants.ORGANIZATIONAL_UNIT_OC);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                    entry.put(SchemaConstants.OU_AT, LdapUtils.OU_GROUPS);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    entry.put(SchemaConstants.DESCRIPTION_AT, description);
                }

                return entry;
            }

            case USER_UNIT: {

                // create users entry
                // dn: ou=users, dc=<domain>
                // objectClass: top
                // objectClass: organizationalUnit
                // ou: users
                // description: <id> Users

                String id = entity.apply(ColumnNames.ID, String.class);
                String description = entity.apply(ColumnNames.DESCRIPTION, String.class);
                Dn dn = LdapUtils.createDn(schemaManager, entityType, id, getId());
                Entry entry = new DefaultEntry(schemaManager, dn);

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OBJECT_CLASS_AT_OID)) {

                    entry.put(SchemaConstants.OBJECT_CLASS_AT,
                            SchemaConstants.TOP_OC,
                            SchemaConstants.ORGANIZATIONAL_UNIT_OC);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.OU_AT_OID)) {

                    entry.put(SchemaConstants.OU_AT, LdapUtils.OU_USERS);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    entry.put(SchemaConstants.DESCRIPTION_AT, description);
                }

                return entry;
            }

            case GROUP: {

                String name = entity.apply(ColumnNames.NAME, String.class);
                String description = entity.apply(ColumnNames.DESCRIPTION, String.class);
                Dn dn = LdapUtils.createDn(schemaManager, entityType, name, getId());
                Entry entry = new DefaultEntry(schemaManager, dn);

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

                    entry.put(SchemaConstants.CN_AT, name);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DESCRIPTION_AT_OID)) {

                    if (description != null && !description.isEmpty())
                        entry.put(SchemaConstants.DESCRIPTION_AT, description);
                }

                return entry;
            }

            case USER: {

                String id = entity.apply(ColumnNames.ID, String.class);
                String username = entity.apply(ColumnNames.USERNAME, String.class);
                String lastName = entity.apply(ColumnNames.LAST_NAME, String.class);
                String firstName = entity.apply(ColumnNames.FIRST_NAME, String.class);
                String displayName = entity.apply(ColumnNames.DISPLAY_NAME, String.class);
                String email = entity.apply(ColumnNames.EMAIL, String.class);
                Dn dn = LdapUtils.createDn(schemaManager, entityType, username, getId());
                Entry entry = new DefaultEntry(schemaManager, dn);

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

                    entry.put(SchemaConstants.UID_AT, id);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.CN_AT_OID)) {

                    entry.put(SchemaConstants.CN_AT, username);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.SN_AT_OID)) {

                    if (lastName != null && !lastName.isEmpty()) {

                        if (serverConfig.isAbbreviateSnAttribute())
                            entry.put(SchemaConstants.SN_AT, lastName);
                        else
                            entry.put(SchemaConstants.SURNAME_AT, lastName);
                    }
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.GN_AT_OID)) {

                    if (firstName != null && !firstName.isEmpty()) {

                        if (serverConfig.isAbbreviateGnAttribute())
                            entry.put(SchemaConstants.GN_AT, firstName);
                        else
                            entry.put(SchemaConstants.GIVENNAME_AT, firstName);
                    }
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.DISPLAY_NAME_AT_OID)) {

                    if (displayName != null && !displayName.isEmpty())
                        entry.put(SchemaConstants.DISPLAY_NAME_AT, displayName);
                }

                if (attributes.isEmpty() || attributes.contains(SchemaConstants.MAIL_AT_OID)) {

                    if (email != null && !email.isEmpty())
                        entry.put(SchemaConstants.MAIL_AT, email);
                }

                return entry;
            }

            default:
                throw new IllegalArgumentException("Cannot create entry for with unknown type " + entityType);
        }
    }

    private void addRelationshipToEntries(Entry entry, Row entity, Set<String> attributes) {

        EntityType entityType = EntityType.fromString(entity.apply(ColumnNames.TYPE, String.class));

        try {

            switch (entityType) {

                case GROUP: {

                    if (attributes.isEmpty() || attributes.contains(SchemaConstants.MEMBER_AT_OID)) {

                        if (!serverConfig.isFlatteningEnabled()) {

                            String memberNameGroup = null;

                            try {

                                memberNameGroup = entity.apply("member_group_name", String.class);

                            } catch (UnknownColumnException e) {

                                logger.trace("Cannot find column member_group_name in group row.");
                            }

                            if (memberNameGroup != null) {

                                Dn dn = LdapUtils.createDn(schemaManager, EntityType.GROUP, memberNameGroup, getId());

                                entry.add(SchemaConstants.MEMBER_AT, dn.getName());
                            }
                        }

                        {
                            String memberNameUser = null;

                            try {

                                memberNameUser = entity.apply("member_user_username", String.class);

                            } catch (UnknownColumnException e) {

                                logger.trace("Cannot find column member_user_username in group row.");
                            }

                            if (memberNameUser != null) {

                                Dn dn = LdapUtils.createDn(schemaManager, EntityType.USER, memberNameUser, getId());

                                entry.add(SchemaConstants.MEMBER_AT, dn.getName());
                            }
                        }
                    }

                    if (attributes.isEmpty() || attributes.contains(LdapUtils.MEMBER_OF_AT_OID)) {

                        if (!serverConfig.isFlatteningEnabled()) {

                            String memberOfName = null;

                            try {

                                memberOfName = entity.apply("parent_group_name", String.class);

                            } catch (UnknownColumnException e) {

                                logger.trace("Cannot find column parent_group_name in group row.");
                            }

                            if (memberOfName != null) {

                                Dn dn = LdapUtils.createDn(schemaManager, EntityType.GROUP, memberOfName, getId());

                                entry.add(LdapUtils.MEMBER_OF_AT, dn.getName());
                            }
                        }
                    }

                    break;
                }

                case USER: {

                    if (attributes.isEmpty() || attributes.contains(LdapUtils.MEMBER_OF_AT_OID)) {

                        String memberOfName = null;

                        try {

                            memberOfName = entity.apply("parent_group_name", String.class);

                        } catch (UnknownColumnException e) {

                            logger.trace("Cannot find column parent_group_name in user row.");
                        }

                        if (memberOfName != null) {

                            Dn dn = LdapUtils.createDn(schemaManager, EntityType.GROUP, memberOfName, getId());

                            entry.add(LdapUtils.MEMBER_OF_AT, dn.getName());
                        }
                    }

                    break;
                }

                default:
                    break;
            }

        } catch (LdapException e) {

            throw new IllegalArgumentException("Cannot handle attributes correctly.", e);
        }
    }

    private MappableCursor<Entry> createEntries(MappableCursor<Row> cursor, Set<String> attributes) {

        return new MappableCursor<Entry>() {

            private boolean initialized = false;
            private String nextId;
            private Entry nextEntry;
            private Entry currentEntry;

            @Override
            public boolean next() {

                if (!initialized) {

                    initialized = true;

                    if (cursor.next()) {

                        nextId = cursor.get().apply(ColumnNames.ID, String.class);
                        nextEntry = createEntry(cursor.get(), attributes);
                        addRelationshipToEntries(nextEntry, cursor.get(), attributes);
                    }
                }

                currentEntry = nextEntry;

                while (cursor.next()) {

                    if (cursor.get().apply(ColumnNames.ID, String.class).equals(nextId)) {

                        addRelationshipToEntries(currentEntry, cursor.get(), attributes);

                    } else {

                        nextId = cursor.get().apply(ColumnNames.ID, String.class);
                        nextEntry = createEntry(cursor.get(), attributes);
                        addRelationshipToEntries(nextEntry, cursor.get(), attributes);
                        break;
                    }
                }

                if (currentEntry == nextEntry) {

                    nextId = null;
                    nextEntry = null;
                }

                return currentEntry != null;
            }

            @Override
            public Entry get() {

                return currentEntry;
            }

            @Override
            public void close()
                    throws IOException {

                cursor.close();
            }
        };
    }
}
