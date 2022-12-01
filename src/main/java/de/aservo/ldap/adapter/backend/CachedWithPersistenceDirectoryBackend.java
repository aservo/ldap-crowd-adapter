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

import de.aservo.ldap.adapter.ServerConfiguration;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.database.CloseableTransaction;
import de.aservo.ldap.adapter.api.database.QueryDefFactory;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.database.result.CursorResult;
import de.aservo.ldap.adapter.api.database.result.IgnoredResult;
import de.aservo.ldap.adapter.api.database.result.IndexedSeqResult;
import de.aservo.ldap.adapter.api.database.result.SingleOptResult;
import de.aservo.ldap.adapter.api.directory.NestedDirectoryBackend;
import de.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import de.aservo.ldap.adapter.api.entity.EntityType;
import de.aservo.ldap.adapter.api.entity.GroupEntity;
import de.aservo.ldap.adapter.api.entity.MembershipEntity;
import de.aservo.ldap.adapter.api.entity.UserEntity;
import de.aservo.ldap.adapter.api.query.QueryExpression;
import de.aservo.ldap.adapter.sql.impl.DatabaseService;
import de.aservo.ldap.adapter.sql.impl.QueryGenerator;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A proxy for directory backends to cache entities via SQL based persistence strategy.
 */
public class CachedWithPersistenceDirectoryBackend
        extends CachedDirectoryBackend {

    /**
     * The constant CONFIG_DB_DRIVER.
     */
    public static final String CONFIG_DB_DRIVER = "database.jdbc.driver";
    /**
     * The constant CONFIG_DB_URL.
     */
    public static final String CONFIG_DB_URL = "database.jdbc.connection.url";
    /**
     * The constant CONFIG_DB_USER.
     */
    public static final String CONFIG_DB_USER = "database.jdbc.connection.user";
    /**
     * The constant CONFIG_DB_PW.
     */
    public static final String CONFIG_DB_PW = "database.jdbc.connection.password";
    /**
     * The constant CONFIG_DB_MIN_IDLE.
     */
    public static final String CONFIG_DB_MIN_IDLE = "database.jdbc.connection.min-idle";
    /**
     * The constant CONFIG_DB_MAX_IDLE.
     */
    public static final String CONFIG_DB_MAX_IDLE = "database.jdbc.connection.max-idle";
    /**
     * The constant CONFIG_DB_MAX_TOTAL.
     */
    public static final String CONFIG_DB_MAX_TOTAL = "database.jdbc.connection.max-total";
    /**
     * The constant CONFIG_DB_MAX_OPEN_STMT.
     */
    public static final String CONFIG_DB_MAX_OPEN_STMT = "database.jdbc.connection.max-open-prepared-statements";
    /**
     * The constant CONFIG_DB_ISO_LEVEL.
     */
    public static final String CONFIG_DB_ISO_LEVEL = "database.jdbc.connection.isolation-level";
    /**
     * The constant CONFIG_TRANSACTION_TIMEOUT.
     */
    public static final String CONFIG_TRANSACTION_TIMEOUT = "persistence.transaction-timeout";
    /**
     * The constant CONFIG_APPLY_NATIVE_SQL.
     */
    public static final String CONFIG_APPLY_NATIVE_SQL = "persistence.apply-native-sql";
    /**
     * The constant CONFIG_USE_MATERIALIZED_VIEWS.
     */
    public static final String CONFIG_USE_MATERIALIZED_VIEWS = "persistence.use-materialized-views";
    /**
     * The constant CONFIG_PASS_ACTIVE_USERS_ONLY.
     */
    public static final String CONFIG_PASS_ACTIVE_USERS_ONLY = "persistence.pass-active-users-only";

    private final Logger logger = LoggerFactory.getLogger(CachedWithPersistenceDirectoryBackend.class);
    private final Map<Long, QueryDefFactory> queryDefFactories = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, CloseableTransactionWrapper> closeableTransactions = Collections.synchronizedMap(new HashMap<>());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final DatabaseService dbService;
    private final int transactionTimeout;
    private final boolean applyNativeSql;
    private final boolean useMaterializedViews;
    private final boolean activeUsersOnly;

    /**
     * Instantiates a new directory backend.
     *
     * @param config           config the config instance of the server
     * @param directoryBackend the directory backend
     */
    public CachedWithPersistenceDirectoryBackend(ServerConfiguration config, NestedDirectoryBackend directoryBackend) {

        super(config, directoryBackend);

        Properties properties = config.getBackendProperties();

        String driver = properties.getProperty(CONFIG_DB_DRIVER);
        String url = properties.getProperty(CONFIG_DB_URL);
        String user = properties.getProperty(CONFIG_DB_USER);
        String password = properties.getProperty(CONFIG_DB_PW);
        String minIdleValue = properties.getProperty(CONFIG_DB_MIN_IDLE);
        String maxIdleValue = properties.getProperty(CONFIG_DB_MAX_IDLE);
        String maxTotalValue = properties.getProperty(CONFIG_DB_MAX_TOTAL);
        String maxOpenPreparedStatementsValue = properties.getProperty(CONFIG_DB_MAX_OPEN_STMT);
        String isolationLevelValue = properties.getProperty(CONFIG_DB_ISO_LEVEL);
        String transactionTimeoutValue = properties.getProperty(CONFIG_TRANSACTION_TIMEOUT);

        if (transactionTimeoutValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_TRANSACTION_TIMEOUT);

        transactionTimeout = Integer.parseInt(transactionTimeoutValue);

        applyNativeSql = Boolean.parseBoolean(properties.getProperty(CONFIG_APPLY_NATIVE_SQL, "false"));
        useMaterializedViews = Boolean.parseBoolean(properties.getProperty(CONFIG_USE_MATERIALIZED_VIEWS, "false"));
        activeUsersOnly = Boolean.parseBoolean(properties.getProperty(CONFIG_PASS_ACTIVE_USERS_ONLY, "true"));

        if (driver == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_DRIVER);

        if (url == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_URL);

        if (user == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_USER);

        if (password == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_PW);

        if (minIdleValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_MIN_IDLE);

        if (maxIdleValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_MAX_IDLE);

        if (maxTotalValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_MAX_TOTAL);

        if (maxOpenPreparedStatementsValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_MAX_OPEN_STMT);

        if (isolationLevelValue == null)
            throw new IllegalArgumentException("Missing value for " + CONFIG_DB_ISO_LEVEL);

        int minIdle = Integer.parseInt(minIdleValue);
        int maxIdle = Integer.parseInt(maxIdleValue);
        int maxTotal = Integer.parseInt(maxTotalValue);
        int maxOpenPreparedStatements = Integer.parseInt(maxOpenPreparedStatementsValue);

        if (minIdle < 1 || maxIdle < 1 || maxTotal < 1 || maxOpenPreparedStatements < 1)
            throw new IllegalArgumentException("Expect connection pool limits greater than one.");

        int isolationLevel;

        if (isolationLevelValue.equalsIgnoreCase("NONE"))
            isolationLevel = Connection.TRANSACTION_NONE;
        else if (isolationLevelValue.equalsIgnoreCase("READ_UNCOMMITTED"))
            isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
        else if (isolationLevelValue.equalsIgnoreCase("READ_COMMITTED"))
            isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
        else if (isolationLevelValue.equalsIgnoreCase("REPEATABLE_READ"))
            isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
        else if (isolationLevelValue.equalsIgnoreCase("SERIALIZABLE"))
            isolationLevel = Connection.TRANSACTION_SERIALIZABLE;
        else
            throw new IllegalArgumentException("Expect valid isolation level.");

        dbService = new DatabaseService(logger, driver, url, user, password, minIdle, maxIdle, maxTotal,
                maxOpenPreparedStatements, isolationLevel, applyNativeSql);
    }

    @Override
    public void startup() {

        super.startup();
        dbService.startup();
        scheduler.scheduleAtFixedRate(this::clearCloseableTransaction, 3, 4, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {

        dbService.shutdown();
        super.shutdown();
    }

    @Override
    public <T> T withReadAccess(Supplier<T> block) {

        return processTransaction(true, block);
    }

    @Override
    public void withReadAccess(Runnable block) {

        withReadAccess(() -> {

            block.run();
            return null;
        });
    }

    @Override
    public <T> T withWriteAccess(Supplier<T> block) {

        return processTransaction(false, () -> {

            T result = block.get();

            if (useMaterializedViews) {

                QueryDefFactory factory = getCurrentQueryDefFactory();

                logger.debug("Starting materialized views refresh.");

                factory
                        .queryById("refresh_materialized_view_for_transitive_group_memberships")
                        .execute(IgnoredResult.class);

                factory
                        .queryById("refresh_materialized_view_for_transitive_user_memberships")
                        .execute(IgnoredResult.class);

                logger.debug("Finished materialized views refresh.");
            }

            return result;
        });
    }

    @Override
    public void withWriteAccess(Runnable block) {

        withWriteAccess(() -> {

            block.run();
            return null;
        });
    }

    @Override
    public boolean requireReset() {

        return dbService.hasUpdatedSchema();
    }

    @Override
    public void upsertGroup(String id) {

        super.upsertGroup(id);

        try {

            GroupEntity entity = directoryBackend.getGroup(id);

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_group")
                    .on("id", entity.getId())
                    .on("name", entity.getName())
                    .on("description", Optional.ofNullable(entity.getDescription()))
                    .execute(IgnoredResult.class);

        } catch (EntityNotFoundException e) {

            logger.warn("The group entity no longer exists.", e);
        }
    }

    @Override
    public int upsertAllGroups(int startIndex, int maxResults) {

        super.upsertAllGroups(startIndex, maxResults);

        Set<GroupEntity> entities = directoryBackend.getAllGroups(startIndex, maxResults);

        entities.forEach(entity -> {

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_group")
                    .on("id", entity.getId())
                    .on("name", entity.getName())
                    .on("description", Optional.ofNullable(entity.getDescription()))
                    .execute(IgnoredResult.class);
        });

        return entities.size();
    }

    @Override
    public int upsertAllGroups() {

        super.upsertAllGroups();

        Set<GroupEntity> entities = directoryBackend.getAllGroups();

        entities.forEach(entity -> {

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_group")
                    .on("id", entity.getId())
                    .on("name", entity.getName())
                    .on("description", Optional.ofNullable(entity.getDescription()))
                    .execute(IgnoredResult.class);
        });

        return entities.size();
    }

    @Override
    public void upsertUser(String id) {

        super.upsertUser(id);

        try {

            UserEntity entity = directoryBackend.getUser(id);

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_user")
                    .on("id", entity.getId())
                    .on("username", entity.getUsername())
                    .on("last_name", Optional.ofNullable(entity.getLastName()))
                    .on("first_name", Optional.ofNullable(entity.getFirstName()))
                    .on("display_name", Optional.ofNullable(entity.getDisplayName()))
                    .on("email", Optional.ofNullable(entity.getEmail()))
                    .on("active", entity.isActive())
                    .execute(IgnoredResult.class);

        } catch (EntityNotFoundException e) {

            logger.warn("The user entity no longer exists.", e);
        }
    }

    @Override
    public void upsertUser(String id, String idOther) {

        super.upsertUser(id, idOther);

        upsertUser(id);

        QueryDefFactory factory = getCurrentQueryDefFactory();

        getDirectGroupsOfUser(idOther).forEach(group -> {

            factory
                    .queryById("create_user_membership_if_not_exists")
                    .on("parent_group_id", group.getName())
                    .on("member_user_id", id)
                    .execute(IgnoredResult.class);
        });
    }

    @Override
    public int upsertAllUsers(int startIndex, int maxResults) {

        super.upsertAllUsers(startIndex, maxResults);

        Set<UserEntity> entities = directoryBackend.getAllUsers(startIndex, maxResults);

        entities.forEach(entity -> {

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_user")
                    .on("id", entity.getId())
                    .on("username", entity.getUsername())
                    .on("last_name", Optional.ofNullable(entity.getLastName()))
                    .on("first_name", Optional.ofNullable(entity.getFirstName()))
                    .on("display_name", Optional.ofNullable(entity.getDisplayName()))
                    .on("email", Optional.ofNullable(entity.getEmail()))
                    .on("active", entity.isActive())
                    .execute(IgnoredResult.class);
        });

        return entities.size();
    }

    @Override
    public int upsertAllUsers() {

        super.upsertAllUsers();

        Set<UserEntity> entities = directoryBackend.getAllUsers();

        entities.forEach(entity -> {

            QueryDefFactory factory = getCurrentQueryDefFactory();

            factory
                    .queryById("create_or_update_user")
                    .on("id", entity.getId())
                    .on("username", entity.getUsername())
                    .on("last_name", Optional.ofNullable(entity.getLastName()))
                    .on("first_name", Optional.ofNullable(entity.getFirstName()))
                    .on("display_name", Optional.ofNullable(entity.getDisplayName()))
                    .on("email", Optional.ofNullable(entity.getEmail()))
                    .on("active", entity.isActive())
                    .execute(IgnoredResult.class);
        });

        return entities.size();
    }

    @Override
    public void upsertMembership(MembershipEntity membership) {

        super.upsertMembership(membership);

        QueryDefFactory factory = getCurrentQueryDefFactory();

        membership.getMemberGroupIds().forEach(id -> {

            factory
                    .queryById("create_group_membership_if_not_exists")
                    .on("parent_group_id", membership.getParentGroupId())
                    .on("member_group_id", id)
                    .execute(IgnoredResult.class);
        });

        membership.getMemberUserIds().forEach(id -> {

            factory
                    .queryById("create_user_membership_if_not_exists")
                    .on("parent_group_id", membership.getParentGroupId())
                    .on("member_user_id", id)
                    .execute(IgnoredResult.class);
        });
    }

    @Override
    public void dropGroup(String id) {

        super.dropGroup(id);

        QueryDefFactory factory = getCurrentQueryDefFactory();

        factory
                .queryById("remove_group_if_exists")
                .on("id", id)
                .execute(IgnoredResult.class);
    }

    @Override
    public void dropAllGroups() {

        super.dropAllGroups();

        QueryDefFactory factory = getCurrentQueryDefFactory();

        factory
                .queryById("remove_all_groups")
                .execute(IgnoredResult.class);
    }

    @Override
    public void dropUser(String id) {

        super.dropUser(id);

        QueryDefFactory factory = getCurrentQueryDefFactory();

        factory
                .queryById("remove_user_if_exists")
                .on("id", id)
                .execute(IgnoredResult.class);
    }

    @Override
    public void dropAllUsers() {

        super.dropAllUsers();

        QueryDefFactory factory = getCurrentQueryDefFactory();

        factory
                .queryById("remove_all_users")
                .execute(IgnoredResult.class);
    }

    @Override
    public void dropMembership(MembershipEntity membership) {

        super.dropMembership(membership);

        QueryDefFactory factory = getCurrentQueryDefFactory();

        membership.getMemberGroupIds().forEach(id -> {

            factory
                    .queryById("remove_group_membership_if_exists")
                    .on("parent_group_id", membership.getParentGroupId())
                    .on("member_group_id", id)
                    .execute(IgnoredResult.class);
        });

        membership.getMemberUserIds().forEach(id -> {

            factory
                    .queryById("remove_user_membership_if_exists")
                    .on("parent_group_id", membership.getParentGroupId())
                    .on("member_user_id", id)
                    .execute(IgnoredResult.class);
        });
    }

    @Override
    public MappableCursor<Row> runQueryExpression(String txId, SchemaManager schemaManager, QueryExpression expression,
                                                  EntityType entityType) {

        QueryGenerator generator =
                new QueryGenerator(schemaManager, getId(), config.isFlatteningEnabled(), activeUsersOnly,
                        useMaterializedViews);

        return addCursorCleanup(txId, generator.generate(entityType, getCloseableTransaction(txId).getQueryDefFactory(),
                        expression)
                .execute(CursorResult.class)
                .transform(Function.identity()));
    }

    @Override
    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_group")
                .on("id", id)
                .execute(SingleOptResult.class)
                .transform(this::mapGroupEntity)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find group in persistent cache."));
    }

    @Override
    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_user")
                .on("id", id)
                .on("active_only", activeUsersOnly)
                .execute(SingleOptResult.class)
                .transform(this::mapUserEntity)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find user in persistent cache."));
    }

    @Override
    public Set<GroupEntity> getAllGroups() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_all_groups")
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity));
    }

    @Override
    public Set<UserEntity> getAllUsers() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_all_users")
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapUserEntity));
    }

    @Override
    public Set<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_direct_users_of_group")
                .on("group_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapUserEntity));
    }

    @Override
    public Set<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_direct_groups_of_user")
                .on("user_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity));
    }

    @Override
    public Set<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_direct_child_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity));
    }

    @Override
    public Set<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return new HashSet<>(factory
                .queryById("find_direct_parent_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity));
    }

    @Override
    public Set<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        if (useMaterializedViews) {

            return new HashSet<>(factory
                    .queryById("find_transitive_users_of_group")
                    .on("group_id", id)
                    .on("active_only", activeUsersOnly)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapUserEntity));

        } else {

            return new HashSet<>(factory
                    .queryById("find_transitive_users_of_group_non_materialized")
                    .on("group_id", id)
                    .on("active_only", activeUsersOnly)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapUserEntity));
        }
    }

    @Override
    public Set<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        if (useMaterializedViews) {

            return new HashSet<>(factory
                    .queryById("find_transitive_groups_of_user")
                    .on("user_id", id)
                    .on("active_only", activeUsersOnly)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));

        } else {

            return new HashSet<>(factory
                    .queryById("find_transitive_groups_of_user_non_materialized")
                    .on("user_id", id)
                    .on("active_only", activeUsersOnly)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));
        }
    }

    @Override
    public Set<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        if (useMaterializedViews) {

            return new HashSet<>(factory
                    .queryById("find_transitive_child_groups_of_group")
                    .on("group_id", id)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));

        } else {

            return new HashSet<>(factory
                    .queryById("find_transitive_child_groups_of_group_non_materialized")
                    .on("group_id", id)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));
        }
    }

    @Override
    public Set<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        if (useMaterializedViews) {

            return new HashSet<>(factory
                    .queryById("find_transitive_parent_groups_of_group")
                    .on("group_id", id)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));

        } else {

            return new HashSet<>(factory
                    .queryById("find_transitive_parent_groups_of_group_non_materialized")
                    .on("group_id", id)
                    .execute(IndexedSeqResult.class)
                    .transform(this::mapGroupEntity));
        }
    }

    private <T> T processTransaction(boolean readOnly, Supplier<T> block) {

        return dbService.withTransaction(factory -> {

            long id = Thread.currentThread().getId();
            T result;

            logger.debug("[Thread ID {}] - Bind query definition factory to thread.", id);

            queryDefFactories.put(id, factory);

            try {

                if (readOnly)
                    result = super.withReadAccess(block);
                else
                    result = super.withWriteAccess(block);

            } finally {

                queryDefFactories.remove(id);
            }

            return result;
        });
    }

    private void clearCloseableTransaction() {

        (new HashMap<>(closeableTransactions)).forEach((txId, transaction) -> {

            if (System.currentTimeMillis() - transaction.timestamp > transactionTimeout) {

                try {

                    transaction.closeUnchecked(new TimeoutException("A transaction was terminated after timeout."));

                } catch (Exception e) {

                    logger.warn("A transaction cleanup was performed.", e);

                } finally {

                    closeableTransactions.remove(txId);
                }
            }
        });
    }

    private CloseableTransaction getCloseableTransaction(String txId) {

        if (closeableTransactions.containsKey(txId))
            closeableTransactions.get(txId).counter.incrementAndGet();
        else
            closeableTransactions.put(txId, new CloseableTransactionWrapper(dbService.getCloseableTransaction()));

        return closeableTransactions.get(txId);
    }

    private QueryDefFactory getCurrentQueryDefFactory() {

        return queryDefFactories.get(Thread.currentThread().getId());
    }

    private MappableCursor<Row> addCursorCleanup(String txId, MappableCursor<Row> rows) {

        return new MappableCursor<Row>() {

            @Override
            public boolean next() {

                return rows.next();
            }

            @Override
            public Row get() {

                return rows.get();
            }

            @Override
            public void close()
                    throws IOException {

                CloseableTransactionWrapper transaction = closeableTransactions.get(txId);
                int count = transaction.counter.decrementAndGet();

                rows.close();

                if (count == 0) {

                    logger.debug("[Thread ID {}] - Close async transaction.", Thread.currentThread().getId());

                    try {

                        transaction.close();

                    } finally {

                        closeableTransactions.remove(txId);
                    }
                }
            }
        };
    }

    private GroupEntity mapGroupEntity(Row row) {

        return new GroupEntity(
                row.apply("name", String.class),
                row.apply("description", String.class));
    }

    private UserEntity mapUserEntity(Row row) {

        return new UserEntity(
                row.apply("username", String.class),
                row.apply("last_name", String.class),
                row.apply("first_name", String.class),
                row.apply("display_name", String.class),
                row.apply("email", String.class),
                row.apply("active", Boolean.class));
    }

    private static class CloseableTransactionWrapper
            implements CloseableTransaction {

        private final CloseableTransaction transaction;

        public final AtomicInteger counter = new AtomicInteger(1);
        public final long timestamp = System.currentTimeMillis();

        public CloseableTransactionWrapper(CloseableTransaction transaction) {

            this.transaction = transaction;
        }

        public QueryDefFactory getQueryDefFactory() {

            return transaction.getQueryDefFactory();
        }

        public void close(Exception cause)
                throws IOException {

            transaction.close(cause);
        }

        public void close()
                throws IOException {

            transaction.close();
        }
    }
}
