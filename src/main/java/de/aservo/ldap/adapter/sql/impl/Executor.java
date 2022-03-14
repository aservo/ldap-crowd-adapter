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

package de.aservo.ldap.adapter.sql.impl;

import com.google.common.collect.Lists;
import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.database.QueryDef;
import de.aservo.ldap.adapter.api.database.QueryDefFactory;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.database.exception.UncheckedSQLException;
import de.aservo.ldap.adapter.api.database.exception.UnknownColumnException;
import de.aservo.ldap.adapter.api.database.result.*;
import org.apache.commons.io.IOUtils;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * The SQL specific database executor.
 */
public class Executor {

    private final Logger logger;
    private final Connection connection;
    private final Map<String, String> clauses;

    public static final String NATIVE_SQL_INDICATOR = "NATIVE_SQL:";

    public Executor(Logger logger, Connection connection, String resourcePath) {

        this.logger = logger;
        this.connection = connection;
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
        long start = System.currentTimeMillis();
        boolean nonStreamed = true;

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

            Query finalQuery = query;

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

                    LinkedHashMap<String, JDBCType> metadata = getMetadata(statement);
                    List<String> columnNames = new ArrayList<>(metadata.keySet());
                    MappableCursor<Row> cursor = getRowCursor(statement, metadata);

                    if (clazz == SingleResult.class) {

                        List<Row> rows = Lists.newArrayList(cursor.iterator(x -> new RowProxyImpl(x, columnNames)));

                        if (rows.size() != 1)
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality of 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new SingleResult() {

                                    public List<String> getColumns() {

                                        return columnNames;
                                    }

                                    public <R> R transform(Function<Row, R> f) {

                                        return rows.stream().map(f).findAny().get();
                                    }
                                };

                    } else if (clazz == SingleOptResult.class) {

                        List<Row> rows = Lists.newArrayList(cursor.iterator(x -> new RowProxyImpl(x, columnNames)));

                        if (rows.size() > 1)
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality of 0 or 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new SingleOptResult() {

                                    public List<String> getColumns() {

                                        return columnNames;
                                    }

                                    public <R> Optional<R> transform(Function<Row, R> f) {

                                        return rows.stream().map(f).findAny();
                                    }
                                };

                    } else if (clazz == IndexedSeqResult.class) {

                        List<Row> rows = Lists.newArrayList(cursor.iterator(x -> new RowProxyImpl(x, columnNames)));

                        concreteResult =
                                new IndexedSeqResult() {

                                    public List<String> getColumns() {

                                        return columnNames;
                                    }

                                    public <R> List<R> transform(Function<Row, R> f) {

                                        return rows.stream().map(f).collect(Collectors.toList());
                                    }
                                };

                    } else if (clazz == IndexedNonEmptySeqResult.class) {

                        List<Row> rows = Lists.newArrayList(cursor.iterator(x -> new RowProxyImpl(x, columnNames)));

                        if (rows.isEmpty())
                            throw new IllegalArgumentException(
                                    "Expect set of rows with cardinality greater than 1 but " +
                                            rows.size() + " received.");

                        concreteResult =
                                new IndexedNonEmptySeqResult() {

                                    public List<String> getColumns() {

                                        return columnNames;
                                    }

                                    public <R> List<R> transform(Function<Row, R> f) {

                                        return rows.stream().map(f).collect(Collectors.toList());
                                    }
                                };

                    } else if (clazz == CursorResult.class) {

                        concreteResult =
                                new CursorResult() {

                                    public List<String> getColumns() {

                                        return columnNames;
                                    }

                                    public <R> MappableCursor<R> transform(Function<Row, R> f) {

                                        MappableCursor<R> underlying = cursor.map(f);

                                        return new MappableCursor<R>() {

                                            boolean closed = false;

                                            public boolean next() {

                                                return underlying.next();
                                            }

                                            public R get() {

                                                return underlying.get();
                                            }

                                            public void close()
                                                    throws IOException {

                                                if (!closed) {

                                                    try {

                                                        statement.close();
                                                        underlying.close();

                                                        if (finalQuery != null)
                                                            finalQuery.close();

                                                    } catch (SQLException | DataAccessException e) {

                                                        throw new IOException("Could not close prepared statement", e);
                                                    }

                                                    closed = true;
                                                }
                                            }
                                        };
                                    }
                                };

                        nonStreamed = false;

                    } else
                        throw new IllegalArgumentException("Unsupported type of result class.");
                }

                return (T) concreteResult;

            } finally {

                if (nonStreamed) {

                    statement.close();
                }
            }

        } finally {

            if (nonStreamed) {

                if (query != null)
                    query.close();

                long end = System.currentTimeMillis();

                logger.debug("[Thread ID {}] - A prepared statement was performed in {} ms.",
                        Thread.currentThread().getId(), end - start == 0 ? 1 : end - start);
            }
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

    private Object getColumnValue(String columnName, ResultSet resultSet, Map<String, JDBCType> metadata)
            throws SQLException {

        Object result;
        String lowerColumnName = columnName.toLowerCase();
        JDBCType jdbcType = metadata.get(lowerColumnName);

        if (jdbcType == null)
            throw new UnknownColumnException("Cannot find column " + lowerColumnName + " in current row.");

        if (jdbcType == JDBCType.NULL)
            result = null;
        else if (jdbcType == JDBCType.BIT)
            result = resultSet.getBoolean(lowerColumnName);
        else if (jdbcType == JDBCType.BOOLEAN)
            result = resultSet.getBoolean(lowerColumnName);
        else if (jdbcType == JDBCType.TINYINT)
            result = resultSet.getByte(lowerColumnName);
        else if (jdbcType == JDBCType.SMALLINT)
            result = resultSet.getShort(lowerColumnName);
        else if (jdbcType == JDBCType.INTEGER)
            result = resultSet.getInt(lowerColumnName);
        else if (jdbcType == JDBCType.BIGINT)
            result = resultSet.getLong(lowerColumnName);
        else if (jdbcType == JDBCType.REAL)
            result = resultSet.getFloat(lowerColumnName);
        else if (jdbcType == JDBCType.FLOAT)
            result = resultSet.getDouble(lowerColumnName);
        else if (jdbcType == JDBCType.DOUBLE)
            result = resultSet.getDouble(lowerColumnName);
        else if (jdbcType == JDBCType.NUMERIC)
            result = resultSet.getBigDecimal(lowerColumnName);
        else if (jdbcType == JDBCType.DECIMAL)
            result = resultSet.getBigDecimal(lowerColumnName);
        else if (jdbcType == JDBCType.DATE)
            result = resultSet.getDate(lowerColumnName).toLocalDate();
        else if (jdbcType == JDBCType.TIME)
            result = resultSet.getTime(lowerColumnName).toLocalTime();
        else if (jdbcType == JDBCType.TIMESTAMP)
            result = resultSet.getTimestamp(lowerColumnName).toLocalDateTime();
        else if (jdbcType == JDBCType.BINARY)
            result = toByteList(resultSet.getBytes(lowerColumnName));
        else if (jdbcType == JDBCType.VARBINARY)
            result = toByteList(resultSet.getBytes(lowerColumnName));
        else if (jdbcType == JDBCType.LONGVARBINARY)
            result = toByteList(resultSet.getBytes(lowerColumnName));
        else if (jdbcType == JDBCType.BLOB)
            result = resultSet.getBytes(lowerColumnName);
        else if (jdbcType == JDBCType.CHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.VARCHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.LONGVARCHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.CLOB)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.NCHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.NVARCHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.LONGNVARCHAR)
            result = resultSet.getString(lowerColumnName);
        else if (jdbcType == JDBCType.NCLOB)
            result = resultSet.getString(lowerColumnName);
        else
            throw new IllegalArgumentException(
                    "Cannot set unsupported JDBC type " + jdbcType.getName() +
                            " for column " + lowerColumnName + ".");

        return result;
    }

    private MappableCursor<Row> getRowCursor(PreparedStatement statement, Map<String, JDBCType> metadata)
            throws SQLException {

        ResultSet resultSet = statement.getResultSet();
        Row row = new RowImpl(resultSet, metadata);

        return new MappableCursor<Row>() {

            public boolean next() {

                try {

                    return resultSet.next();

                } catch (SQLException e) {

                    throw new UncheckedSQLException(e);
                }
            }

            public Row get() {

                return row;
            }
        };
    }

    private LinkedHashMap<String, JDBCType> getMetadata(PreparedStatement statement)
            throws SQLException {

        LinkedHashMap<String, JDBCType> result = new LinkedHashMap<>();
        ResultSetMetaData metadata = statement.getMetaData();

        if (metadata == null)
            return result;

        for (int i = 1; i <= metadata.getColumnCount(); i++) {

            String lowerColumnName = metadata.getColumnLabel(i).toLowerCase();

            if (result.containsKey(lowerColumnName))
                throw new IllegalArgumentException("Expect unique column name for resulting rows.");

            result.put(lowerColumnName, JDBCType.valueOf(metadata.getColumnType(i)));
        }

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

    private class RowImpl
            implements Row {

        private final ResultSet resultSet;
        private final Map<String, JDBCType> metadata;

        public RowImpl(ResultSet resultSet, Map<String, JDBCType> metadata) {

            this.resultSet = resultSet;
            this.metadata = metadata;
        }

        public <T> T apply(String columnName, Class<T> clazz) {

            Object result;

            try {

                result = getColumnValue(columnName, resultSet, metadata);

            } catch (SQLException e) {

                throw new UncheckedSQLException(e);
            }

            if (result == null)
                return null;

            try {

                return (T) result;

            } catch (ClassCastException e) {

                throw new IllegalArgumentException(
                        "Cannot perform a read conversion with column " + columnName +
                                " and with type [" + clazz.getName() + "].", e);
            }
        }
    }

    private class RowProxyImpl
            implements Row {

        private final Map<String, Object> underlying = new LinkedHashMap<>();

        public RowProxyImpl(Row row, List<String> columnNames) {

            columnNames.forEach(x -> {

                underlying.put(x, row.apply(x, Object.class));
            });
        }

        public <T> T apply(String columnName, Class<T> clazz) {

            String lowerColumnName = columnName;

            if (!underlying.containsKey(lowerColumnName))
                throw new UnknownColumnException("Cannot find column " + lowerColumnName + " in current row.");

            Object result = underlying.get(lowerColumnName);

            if (result == null)
                return null;

            try {

                return (T) result;

            } catch (ClassCastException e) {

                throw new IllegalArgumentException(
                        "Cannot perform a read conversion with column " + columnName +
                                " and with type [" + clazz.getName() + "].", e);
            }
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

        public QueryDefImpl on(String key, Object value) {

            Map<String, Object> parameters = new HashMap<>(this.parameters);

            parameters.put(key, value);

            return new QueryDefImpl(clauseOrId, parameters, byId);
        }

        public QueryDef on(List<Object> arguments) {

            Map<String, Object> parameters = new HashMap<>(this.parameters);
            int index = 1;

            for (Object argument : arguments)
                parameters.put(Integer.toString(index++), argument);

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

        @Override
        public boolean equals(Object that) {

            if (this == that)
                return true;

            if (that == null || this.getClass() != that.getClass())
                return false;

            QueryDefImpl queryDef = (QueryDefImpl) that;

            return byId == queryDef.byId &&
                    Objects.equals(clauseOrId, queryDef.clauseOrId) &&
                    Objects.equals(parameters, queryDef.parameters);
        }

        @Override
        public int hashCode() {

            return Objects.hash(clauseOrId, parameters, byId);
        }
    }
}
