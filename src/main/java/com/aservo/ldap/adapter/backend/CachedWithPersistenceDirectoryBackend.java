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

package com.aservo.ldap.adapter.backend;

import com.aservo.ldap.adapter.api.FilterMatcher;
import com.aservo.ldap.adapter.api.database.QueryDefFactory;
import com.aservo.ldap.adapter.api.database.Row;
import com.aservo.ldap.adapter.api.database.result.IgnoredResult;
import com.aservo.ldap.adapter.api.database.result.IndexedSeqResult;
import com.aservo.ldap.adapter.api.database.result.SingleOptResult;
import com.aservo.ldap.adapter.api.entity.GroupEntity;
import com.aservo.ldap.adapter.api.entity.MembershipEntity;
import com.aservo.ldap.adapter.api.entity.UserEntity;
import com.aservo.ldap.adapter.api.query.QueryExpression;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.sql.impl.DatabaseService;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import java.sql.Connection;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
     * The constant CONFIG_PASS_ACTIVE_USERS_ONLY.
     */
    public static final String CONFIG_PASS_ACTIVE_USERS_ONLY = "pass-active-users-only";

    private final Logger logger = LoggerFactory.getLogger(CachedWithPersistenceDirectoryBackend.class);
    private final Map<Long, QueryDefFactory> queryDefFactories = Collections.synchronizedMap(new HashMap<>());
    private final DatabaseService dbService;
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
                maxOpenPreparedStatements, isolationLevel);
    }

    @Override
    public void startup() {

        super.startup();
        dbService.startup();
    }

    @Override
    public void shutdown() {

        dbService.shutdown();
        super.shutdown();
    }

    @Override
    public <T> T withReadAccess(Supplier<T> block) {

        return processTransaction(block);
    }

    @Override
    public void withReadAccess(Runnable block) {

        processTransaction(() -> {

            block.run();
            return null;
        });
    }

    @Override
    public <T> T withWriteAccess(Supplier<T> block) {

        return processTransaction(() -> {

            T result = block.get();

            QueryDefFactory factory = getCurrentQueryDefFactory();

            logger.debug("Starting materialized views refresh.");

            factory
                    .queryById("refresh_materialized_view_for_transitive_group_memberships")
                    .execute(IgnoredResult.class);

            factory
                    .queryById("refresh_materialized_view_for_transitive_user_memberships")
                    .execute(IgnoredResult.class);

            logger.debug("Finished materialized views refresh.");

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

        List<GroupEntity> entities = directoryBackend.getAllGroups(startIndex, maxResults);

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

        List<GroupEntity> entities = directoryBackend.getAllGroups();

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
    public int upsertAllUsers(int startIndex, int maxResults) {

        super.upsertAllUsers(startIndex, maxResults);

        List<UserEntity> entities = directoryBackend.getAllUsers(startIndex, maxResults);

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

        List<UserEntity> entities = directoryBackend.getAllUsers();

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
    public List<Pair<String, String>> getAllDirectGroupRelationships() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_all_direct_group_memberships")
                .execute(IndexedSeqResult.class)
                .transform(row -> Pair.of(
                        row.apply("parent_group_id", String.class),
                        row.apply("member_group_id", String.class)
                ));
    }

    @Override
    public List<Pair<String, String>> getAllDirectUserRelationships() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_all_direct_user_memberships")
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> Pair.of(
                        row.apply("parent_group_id", String.class),
                        row.apply("member_user_id", String.class)
                ));
    }

    @Override
    public List<Pair<String, String>> getAllTransitiveGroupRelationships() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        Map<String, Set<String>> relationships = new HashMap<>();

        return factory
                .queryById("find_all_transitive_group_memberships")
                .execute(IndexedSeqResult.class)
                .transform(row -> Pair.of(
                        row.apply("parent_group_id", String.class),
                        row.apply("member_group_id", String.class)
                ));
    }

    @Override
    public List<Pair<String, String>> getAllTransitiveUserRelationships() {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_all_transitive_user_memberships")
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> Pair.of(
                        row.apply("parent_group_id", String.class),
                        row.apply("member_user_id", String.class)
                ));
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
    public List<GroupEntity> getGroups(QueryExpression expression, Optional<FilterMatcher> filterMatcher) {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        // TODO: Transform expression to plain SQL and use factory.query(String clause) to improve performance.

        return factory
                .queryById("find_all_groups")
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity)
                .stream()
                .filter(x -> filterMatcher.map(y -> y.matchEntity(x, expression)).orElse(true))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserEntity> getUsers(QueryExpression expression, Optional<FilterMatcher> filterMatcher) {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        // TODO: Transform expression to plain SQL and use factory.query(String clause) to improve performance.

        return factory
                .queryById("find_all_users")
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapUserEntity)
                .stream()
                .filter(x -> filterMatcher.map(y -> y.matchEntity(x, expression)).orElse(true))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_users_of_group")
                .on("group_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapUserEntity);
    }

    @Override
    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_groups_of_user")
                .on("user_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_child_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_parent_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<String> getDirectUserNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_users_of_group")
                .on("group_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("username", String.class));
    }

    @Override
    public List<String> getDirectGroupNamesOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_groups_of_user")
                .on("user_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public List<String> getDirectChildGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_child_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public List<String> getDirectParentGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_direct_parent_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_users_of_group")
                .on("group_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapUserEntity);
    }

    @Override
    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_groups_of_user")
                .on("user_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_child_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_parent_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(this::mapGroupEntity);
    }

    @Override
    public List<String> getTransitiveUserNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_users_of_group")
                .on("group_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("username", String.class));
    }

    @Override
    public List<String> getTransitiveGroupNamesOfUser(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_groups_of_user")
                .on("user_id", id)
                .on("active_only", activeUsersOnly)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public List<String> getTransitiveChildGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_child_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public List<String> getTransitiveParentGroupNamesOfGroup(String id)
            throws EntityNotFoundException {

        QueryDefFactory factory = getCurrentQueryDefFactory();

        return factory
                .queryById("find_transitive_parent_groups_of_group")
                .on("group_id", id)
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("name", String.class));
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        try {

            return getDirectChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        try {

            return getDirectUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isGroupTransitiveGroupMember(String groupId1, String groupId2) {

        try {

            return getTransitiveChildGroupsOfGroup(groupId2).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(groupId1));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    @Override
    public boolean isUserTransitiveGroupMember(String userId, String groupId) {

        try {

            return getTransitiveUsersOfGroup(groupId).stream()
                    .anyMatch(x -> x.getId().equalsIgnoreCase(userId));

        } catch (EntityNotFoundException e) {

            return false;
        }
    }

    private <T> T processTransaction(Supplier<T> block) {

        return dbService.withTransaction(factory -> {

            long id = Thread.currentThread().getId();
            T result;

            logger.debug("[Thread ID {}] - Bind query definition factory to thread.", id);

            queryDefFactories.put(id, factory);

            try {

                result = super.withReadAccess(block);

            } finally {

                queryDefFactories.remove(id);
            }

            return result;
        });
    }

    private QueryDefFactory getCurrentQueryDefFactory() {

        return queryDefFactories.get(Thread.currentThread().getId());
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
}
