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

import com.aservo.ldap.adapter.api.database.QueryDef;
import com.aservo.ldap.adapter.api.database.QueryDefFactory;
import com.aservo.ldap.adapter.api.database.Row;
import com.aservo.ldap.adapter.api.database.exception.UncheckedSQLException;
import com.aservo.ldap.adapter.api.database.result.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.slf4j.Logger;


/**
 * The SQL specific database executor.
 */
public class Executor {

    private final Logger logger;
    private final Connection connection;
    private final Converter converter;
    private final Map<String, String> clauses;

    private static final String NATIVE_SQL_INDICATOR = "NATIVE_SQL:";

    public Executor(Logger logger, Connection connection, Converter converter, String resourcePath) {

        this.logger = logger;
        this.connection = connection;
        this.converter = converter;
        this.clauses = parseSqlFile(resourcePath);
    }

    public <T extends Result> T executeById(String clauseId, Map<String, Object> parameters, Class<T> clazz)
            throws SQLException {

        String clause = clauses.get(clauseId);

        if (clause == null)
            throw new IllegalArgumentException("Cannot find clause with ID " + clauseId);

        return execute(clause, parameters, clazz);
    }

    public <T extends Result> T execute(String clause, Map<String, Object> parameters, Class<T> clazz)
            throws SQLException {

        Query query = null;
        String sql = null;
        long start = Instant.now().toEpochMilli();

        String trimmedClause = clause.trim();

        if (trimmedClause.startsWith(NATIVE_SQL_INDICATOR)) {

            sql = trimmedClause.substring(NATIVE_SQL_INDICATOR.length()).trim();

        } else {

            logger.debug("[Thread ID {}] - Parse dialect free SQL statement:\n{}",
                    Thread.currentThread().getId(), trimmedClause);

            query = DSL.using(connection).parser().parseQuery(trimmedClause);
        }

        try {

            if (query != null) {

                findBinding(query, parameters);

                sql = query.getSQL();

            }

            logger.debug("[Thread ID {}] - Apply dialect specific SQL statement:\n{}",
                    Thread.currentThread().getId(), sql);

            PreparedStatement statement = connection.prepareStatement(sql);

            try {

                Result concreteResult;

                if (query != null) {

                    setValues(statement, query.getBindValues());

                } else {
                    // TODO: support parameters for native SQL statements
                }

                statement.execute();

                if (clazz == IgnoredResult.class) {

                    concreteResult =
                            new IgnoredResult() {
                            };

                } else {

                    List<Pair<String, JDBCType>> metaData = getMetaData(statement);
                    List<LinkedHashMap<String, Object>> rows = getValues(statement, metaData);
                    List<String> columnNames = metaData.stream().map(Pair::getLeft).collect(Collectors.toList());

                    if (clazz == SingleResult.class) {

                        if (rows.size() != 1)
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality of 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new SingleResult() {

                                    public List<String> getColumns() {

                                        return new ArrayList<>(columnNames);
                                    }

                                    public <R> R transform(Function<Row, R> f) {

                                        return rows.stream()
                                                .map(x -> new RowImpl(converter, x))
                                                .map(f)
                                                .findFirst().get();
                                    }
                                };

                    } else if (clazz == SingleOptResult.class) {

                        if (rows.size() > 1)
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality of 0 or 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new SingleOptResult() {

                                    public List<String> getColumns() {

                                        return new ArrayList<>(columnNames);
                                    }

                                    public <R> Optional<R> transform(Function<Row, R> f) {

                                        return rows.stream()
                                                .map(x -> new RowImpl(converter, x))
                                                .map(f)
                                                .findFirst();
                                    }
                                };

                    } else if (clazz == IndexedSeqResult.class) {

                        concreteResult =
                                new IndexedSeqResult() {

                                    public List<String> getColumns() {

                                        return new ArrayList<>(columnNames);
                                    }

                                    public <R> List<R> transform(Function<Row, R> f) {

                                        return rows.stream()
                                                .map(x -> new RowImpl(converter, x))
                                                .map(f)
                                                .collect(Collectors.toList());
                                    }
                                };

                    } else if (clazz == IndexedNonEmptySeqResult.class) {

                        if (rows.isEmpty())
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality greater than 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new IndexedNonEmptySeqResult() {

                                    public List<String> getColumns() {

                                        return new ArrayList<>(columnNames);
                                    }

                                    public <R> List<R> transform(Function<Row, R> f) {

                                        return rows.stream()
                                                .map(x -> new RowImpl(converter, x))
                                                .map(f)
                                                .collect(Collectors.toList());
                                    }
                                };

                    } else
                        throw new IllegalArgumentException("Unsupported type of result class.");
                }

                return (T) concreteResult;

            } finally {

                statement.close();
            }

        } finally {

            if (query != null)
                query.close();

            long end = Instant.now().toEpochMilli();

            logger.debug("[Thread ID {}] - A prepared statement was performed in {} ms.",
                    Thread.currentThread().getId(), end - start == 0 ? 1 : end - start);
        }
    }

    public QueryDefFactory newQueryDefFactory() {

        return new QueryDefFactory() {

            public QueryDef queryById(String clauseId) {

                return new QueryDefImpl(clauseId, Collections.emptyMap(), true);
            }

            public QueryDef query(String clause) {

                return new QueryDefImpl(clause, Collections.emptyMap(), false);
            }
        };
    }

    public Connection getConnection() {

        return connection;
    }

    public Converter getConverter() {

        return converter;
    }

    private void findBinding(Query query, Map<String, Object> parameters) {

        parameters.forEach((k, v) -> findBinding(query, k, v));
    }

    private void findBinding(Query query, String key, Object value) {

        if (value == null)
            query.bind(key, null);
        else if (value instanceof Character)
            query.bind(key, value.toString());
        else if (value instanceof LocalDate)
            query.bind(key, java.sql.Date.valueOf((LocalDate) value));
        else if (value instanceof LocalTime)
            query.bind(key, java.sql.Time.valueOf((LocalTime) value));
        else if (value instanceof LocalDateTime)
            query.bind(key, java.sql.Timestamp.valueOf((LocalDateTime) value));
        else if (isByteSequence(value))
            query.bind(key, toByteArray(value));
        else if (value instanceof Optional)
            findBinding(query, key, ((Optional<?>) value).orElse(null));
        else
            query.bind(key, value);
    }

    private void setValues(PreparedStatement statement, List<Object> parameters)
            throws SQLException {

        int key = 0;

        for (Object x : parameters) {

            key++;
            setValue(statement, key, x);
        }
    }

    private void setValue(PreparedStatement statement, int key, Object value)
            throws SQLException {

        if (value == null)
            statement.setNull(key, JDBCType.NULL.getVendorTypeNumber());
        else if (value instanceof Boolean)
            statement.setBoolean(key, (boolean) value);
        else if (value instanceof Byte)
            statement.setByte(key, (byte) value);
        else if (value instanceof Short)
            statement.setShort(key, (short) value);
        else if (value instanceof Integer)
            statement.setInt(key, (int) value);
        else if (value instanceof Long)
            statement.setLong(key, (long) value);
        else if (value instanceof Float)
            statement.setFloat(key, (float) value);
        else if (value instanceof Double)
            statement.setDouble(key, (double) value);
        else if (value instanceof Character)
            statement.setString(key, String.valueOf((char) value));
        else if (value instanceof String)
            statement.setString(key, (String) value);
        else if (value instanceof BigDecimal)
            statement.setBigDecimal(key, (BigDecimal) value);
        else if (value instanceof LocalDate)
            statement.setDate(key, java.sql.Date.valueOf((LocalDate) value));
        else if (value instanceof LocalTime)
            statement.setTime(key, java.sql.Time.valueOf((LocalTime) value));
        else if (value instanceof LocalDateTime)
            statement.setTimestamp(key, java.sql.Timestamp.valueOf((LocalDateTime) value));
        else if (value instanceof java.sql.Date)
            statement.setDate(key, (java.sql.Date) value);
        else if (value instanceof java.sql.Time)
            statement.setTime(key, (java.sql.Time) value);
        else if (value instanceof java.sql.Timestamp)
            statement.setTimestamp(key, (java.sql.Timestamp) value);
        else if (isByteSequence(value))
            statement.setBytes(key, toByteArray(value));
        else if (value instanceof Optional)
            setValue(statement, key, ((Optional<?>) value).orElse(null));
        else
            throw new IllegalArgumentException(
                    "Cannot put unsupported type with value " + value + " (" + value.getClass().getName() +
                            ") at key " + key + ".");
    }

    private List<LinkedHashMap<String, Object>> getValues(PreparedStatement statement,
                                                          List<Pair<String, JDBCType>> metaData)
            throws SQLException {

        ResultSet result = statement.getResultSet();
        List<LinkedHashMap<String, Object>> buffer = new ArrayList<>();

        if (result != null) {

            while (result.next()) {

                LinkedHashMap<String, Object> mapping = new LinkedHashMap<>();

                for (Pair<String, JDBCType> column : metaData) {

                    if (column.getRight() == JDBCType.NULL)
                        mapping.put(column.getKey(), null);
                    else if (column.getValue() == JDBCType.BIT)
                        mapping.put(column.getKey(), result.getBoolean(column.getKey()));
                    else if (column.getValue() == JDBCType.BOOLEAN)
                        mapping.put(column.getKey(), result.getBoolean(column.getKey()));
                    else if (column.getValue() == JDBCType.TINYINT)
                        mapping.put(column.getKey(), result.getByte(column.getKey()));
                    else if (column.getValue() == JDBCType.SMALLINT)
                        mapping.put(column.getKey(), result.getShort(column.getKey()));
                    else if (column.getValue() == JDBCType.INTEGER)
                        mapping.put(column.getKey(), result.getInt(column.getKey()));
                    else if (column.getValue() == JDBCType.BIGINT)
                        mapping.put(column.getKey(), result.getLong(column.getKey()));
                    else if (column.getValue() == JDBCType.REAL)
                        mapping.put(column.getKey(), result.getFloat(column.getKey()));
                    else if (column.getValue() == JDBCType.FLOAT)
                        mapping.put(column.getKey(), result.getDouble(column.getKey()));
                    else if (column.getValue() == JDBCType.DOUBLE)
                        mapping.put(column.getKey(), result.getDouble(column.getKey()));
                    else if (column.getValue() == JDBCType.NUMERIC)
                        mapping.put(column.getKey(), result.getBigDecimal(column.getKey()));
                    else if (column.getValue() == JDBCType.DECIMAL)
                        mapping.put(column.getKey(), result.getBigDecimal(column.getKey()));
                    else if (column.getValue() == JDBCType.DATE)
                        mapping.put(column.getKey(), result.getDate(column.getKey()).toLocalDate());
                    else if (column.getValue() == JDBCType.TIME)
                        mapping.put(column.getKey(), result.getTime(column.getKey()).toLocalTime());
                    else if (column.getValue() == JDBCType.TIMESTAMP)
                        mapping.put(column.getKey(), result.getTimestamp(column.getKey()).toLocalDateTime());
                    else if (column.getValue() == JDBCType.BINARY)
                        mapping.put(column.getKey(), toByteList(result.getBytes(column.getKey())));
                    else if (column.getValue() == JDBCType.VARBINARY)
                        mapping.put(column.getKey(), toByteList(result.getBytes(column.getKey())));
                    else if (column.getValue() == JDBCType.LONGVARBINARY)
                        mapping.put(column.getKey(), toByteList(result.getBytes(column.getKey())));
                    else if (column.getValue() == JDBCType.BLOB)
                        mapping.put(column.getKey(), result.getBytes(column.getKey()));
                    else if (column.getValue() == JDBCType.CHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.VARCHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.LONGVARCHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.CLOB)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.NCHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.NVARCHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.LONGNVARCHAR)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else if (column.getValue() == JDBCType.NCLOB)
                        mapping.put(column.getKey(), result.getString(column.getKey()));
                    else
                        throw new IllegalArgumentException(
                                "Cannot set unsupported JDBC type " + column.getValue().getName() +
                                        " for column " + column.getKey() + ".");
                }

                buffer.add(mapping);
            }
        }

        return buffer;
    }

    private List<Pair<String, JDBCType>> getMetaData(PreparedStatement statement)
            throws SQLException {

        List<Pair<String, JDBCType>> result = new ArrayList<>();
        ResultSetMetaData metaData = statement.getMetaData();

        if (metaData == null)
            return Collections.emptyList();

        for (int i = 1; i <= metaData.getColumnCount(); i++)
            result.add(Pair.of(metaData.getColumnLabel(i).toLowerCase(), JDBCType.valueOf(metaData.getColumnType(i))));

        return result;
    }

    private boolean isByteSequence(Object obj) {

        if (obj instanceof Collection<?>) {

            Collection<?> collection = (Collection<?>) obj;

            return collection.stream().allMatch(x -> x instanceof Byte);

        } else if (obj.getClass().isArray()) {

            int length = Array.getLength(obj);

            for (int i = 0; i < length; i++)
                if (!(Array.get(obj, i) instanceof Byte))
                    return false;

            return true;
        }

        return false;
    }

    private byte[] toByteArray(Object obj) {

        if (obj instanceof Collection<?>) {

            Collection<?> collection = (Collection<?>) obj;
            Iterator<?> iter = collection.iterator();
            byte[] array = new byte[collection.size()];

            for (int i = 0; iter.hasNext(); i++)
                array[i] = (Byte) iter.next();

            return array;

        } else if (obj.getClass().isArray()) {

            int length = Array.getLength(obj);
            byte[] array = new byte[length];

            for (int i = 0; i < length; i++)
                array[i] = (byte) Array.get(obj, i);

            return array;
        }

        throw new IllegalArgumentException("Cannot create byte array from object.");
    }

    private List<Byte> toByteList(byte[] array) {

        List<Byte> list = new ArrayList<>(array.length);

        for (byte b : array)
            list.add(b);

        return list;
    }

    private Map<String, String> parseSqlFile(String resourcePath) {

        String headerStart = "--[ID:";
        String headerEnd = "]--";
        Map<String, String> mapping = new HashMap<>();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {

            String result = IOUtils.toString(stream, StandardCharsets.UTF_8.name());

            int pos = result.indexOf(headerStart);

            while (pos != -1) {

                int endPos = result.indexOf(headerEnd, pos + headerStart.length());

                if (endPos == -1)
                    throw new IllegalArgumentException("Missing ending of all clause headers.");

                String id = result.substring(pos + headerStart.length(), endPos).trim();

                if (id.isEmpty())
                    throw new IllegalArgumentException("Expect an ID for all clauses.");

                pos = result.indexOf(headerStart, endPos + headerEnd.length());
                String clause;

                if (pos == -1)
                    clause = result.substring(endPos + headerEnd.length()).trim();
                else
                    clause = result.substring(endPos + headerEnd.length(), pos).trim();

                if (clause.isEmpty())
                    throw new IllegalArgumentException("Expect non empty clauses.");

                mapping.put(id, clause);
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        return mapping;
    }

    private static class RowImpl
            implements Row {

        private final Converter converter;
        private final Map<String, Object> row;

        public RowImpl(Converter converter, Map<String, Object> row) {

            this.converter = converter;
            this.row = row;
        }

        public <T> T apply(String column, Class<T> clazz) {

            Object entry =
                    find(column)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot find column " + column + "."))
                            .orElse(null);

            if (entry == null)
                return null;

            T result =
                    converter.read(entry, clazz)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Cannot perform a read conversion with column " + column +
                                            " and with type [" + clazz.getName() + "]."));

            return result;
        }

        private Optional<Optional<Object>> find(String column) {

            return row.entrySet().stream()
                    .filter(x -> x.getKey().equalsIgnoreCase(column))
                    .map(x -> Optional.ofNullable(x.getValue()))
                    .findAny();
        }
    }

    private class QueryDefImpl
            implements QueryDef {

        private final String clauseOrId;
        private final Map<String, Object> parameters;
        private final boolean byId;

        public QueryDefImpl(String clauseOrId, Map<String, Object> parameters, boolean byId) {

            this.clauseOrId = clauseOrId;
            this.parameters = parameters;
            this.byId = byId;
        }

        public <T> QueryDefImpl on(String key, T value) {

            Map<String, Object> parameters = new HashMap<>(this.parameters);

            Object result = Executor.this.getConverter().write(value)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cannot perform a write conversion with key " + key +
                                    " and with type [" + value.getClass().getName() + "]."));

            parameters.put(key, result);

            return new QueryDefImpl(clauseOrId, parameters, byId);
        }

        public <T extends Result> T execute(Class<T> clazz) {

            try {

                if (byId)
                    return Executor.this.executeById(clauseOrId, parameters, clazz);
                else
                    return Executor.this.execute(clauseOrId, parameters, clazz);

            } catch (SQLException e) {

                throw new UncheckedSQLException(e);
            }
        }
    }
}
