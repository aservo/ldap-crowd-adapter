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

package com.aservo.ldap.adapter.sql.impl;

import com.aservo.ldap.adapter.sql.api.QueryDefFactory;
import com.aservo.ldap.adapter.sql.api.Transactional;
import com.aservo.ldap.adapter.sql.api.exception.UncheckedSQLException;
import com.aservo.ldap.adapter.sql.api.result.IgnoredResult;
import com.aservo.ldap.adapter.sql.api.result.IndexedSeqResult;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

/**
 * A facade for managed database access.
 */
public class DatabaseService
        implements Transactional {

    private final Logger logger;
    private final BasicDataSource dataSource;
    private boolean updatedSchema = false;
    private static final String QUERIES_CLAUSES = "com/aservo/ldap/adapter/db/queries.sql";
    private static final String VERSIONING_SCHEMA_CLAUSES = "com/aservo/ldap/adapter/db/versioning-schema.sql";
    private static final String CREATE_SCHEMA_CLAUSES = "com/aservo/ldap/adapter/db/create-schema.sql";
    private static final String DROP_SCHEMA_CLAUSES = "com/aservo/ldap/adapter/db/drop-schema.sql";

    /**
     * Instantiates a new DatabaseService.
     *
     * @param logger                    the logger instance
     * @param driver                    the database driver
     * @param url                       the URL to the endpoint of the database
     * @param user                      the name of the database user
     * @param password                  the password of the database user
     * @param minIdle                   the minimum number of idle connections used for connection pooling
     * @param maxIdle                   the maximum number of idle connections used for connection pooling
     * @param maxTotal                  the maximum number of total connections used for connection pooling
     * @param maxOpenPreparedStatements the maximum number of open prepared statements
     * @param isolationLevel            the isolation level used for transactions
     */
    public DatabaseService(Logger logger, String driver, String url, String user, String password, int minIdle,
                           int maxIdle, int maxTotal, int maxOpenPreparedStatements, int isolationLevel) {

        this.logger = logger;

        dataSource = new BasicDataSource();

        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxIdle(maxIdle);
        dataSource.setMaxTotal(maxTotal);
        dataSource.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
        dataSource.setDefaultTransactionIsolation(isolationLevel);

        System.setProperty("org.jooq.no-logo", "true");
    }

    /**
     * Startup method.
     */
    public void startup() {

        Connection connection;

        try {

            connection = dataSource.getConnection();

        } catch (SQLException e) {

            throw new UncheckedSQLException("Could not create connection from pool.", e);
        }

        Executor executor = new Executor(logger, connection, new NoopConverter(), VERSIONING_SCHEMA_CLAUSES);

        performSchemaEvolution(executor.newQueryDefFactory());
    }

    /**
     * Shutdown method.
     */
    public void shutdown() {

        try {

            dataSource.close();

        } catch (SQLException e) {

            throw new UncheckedSQLException("Cannot release connection pool.", e);
        }
    }

    /**
     * Check if the schema was updated.
     *
     * @return the boolean
     */
    public boolean hasUpdatedSchema() {

        return updatedSchema;
    }

    @Override
    public <T> T withTransaction(Function<QueryDefFactory, T> block) {

        Connection connection;

        try {

            connection = dataSource.getConnection();

        } catch (SQLException e) {

            throw new UncheckedSQLException("Could not create connection from pool.", e);
        }

        Executor executor = new Executor(logger, connection, new NoopConverter(), QUERIES_CLAUSES);
        long start = Instant.now().toEpochMilli();
        T result;

        try {

            executor.getConnection().setAutoCommit(false);

            result = block.apply(executor.newQueryDefFactory());

            executor.getConnection().commit();

        } catch (Exception e1) {

            try {

                executor.getConnection().rollback();

                throw e1;

            } catch (SQLException e2) {

                throw new UncheckedSQLException(e2);
            }

        } finally {

            try {

                executor.getConnection().close();

            } catch (SQLException e) {

                logger.error("Cannot close database connection", e);
            }
        }

        long end = Instant.now().toEpochMilli();

        logger.debug("[Thread ID {}] - A transaction was performed in {} ms.",
                Thread.currentThread().getId(), end - start == 0 ? 1 : end - start);

        return result;
    }

    public void withTransaction(Consumer<QueryDefFactory> block) {

        withTransaction(x -> {

            block.accept(x);
            return null;
        });
    }

    private void performSchemaEvolution(QueryDefFactory factory) {

        factory
                .queryById("create_schema_version_table")
                .execute(IgnoredResult.class);

        if (!isSchemaUpToDate(factory)) {

            renewSchema(factory);
            updatedSchema = true;
        }
    }

    private boolean isSchemaUpToDate(QueryDefFactory factory) {

        List<Byte> hash = getSchemaVersion();

        return factory
                .queryById("get_schema_version")
                .execute(IndexedSeqResult.class)
                .transform(row -> row.apply("hash", List.class))
                .stream()
                .findFirst()
                .map(x -> x.equals(hash))
                .orElse(false);
    }

    private void renewSchema(QueryDefFactory factory) {

        List<Byte> hash = getSchemaVersion();

        runBatch(factory, DROP_SCHEMA_CLAUSES);
        runBatch(factory, CREATE_SCHEMA_CLAUSES);

        factory
                .queryById("set_schema_version")
                .on("hash", hash)
                .on("created_at", LocalDateTime.now().withNano(0))
                .execute(IgnoredResult.class);
    }

    private List<Byte> getSchemaVersion() {

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(CREATE_SCHEMA_CLAUSES)) {

            String result = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Byte[] hash = ArrayUtils.toObject(digest.digest(result.getBytes(StandardCharsets.UTF_8)));

            return new ArrayList<>(ImmutableList.copyOf(hash));

        } catch (IOException e) {

            throw new UncheckedIOException(e);

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(e);
        }
    }

    private void runBatch(QueryDefFactory factory, String resourcePath) {

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {

            String result = IOUtils.toString(stream, StandardCharsets.UTF_8.name());

            Arrays.stream(result.split(";"))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .forEach(queryClause -> {

                        factory
                                .query(queryClause.trim())
                                .execute(IgnoredResult.class);
                    });

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }
}
