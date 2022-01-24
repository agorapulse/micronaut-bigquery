/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.bigquery;

import com.google.common.base.CaseFormat;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BigQuery service is used to execute SQL statements against BigQuery data warehouse.
 */
public interface BigQueryService {

    /**
     * Runs a SQL query against the BigQuery warehouse and map the results into an object.
     * @param namedParameters the named parameters for the SQL query
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the function mapping the result into an object
     * @param <T> type of the result objects
     * @return the flowable of objects mapped using the builder
     */
    <T> Flowable<T> query(Map<String, ?> namedParameters, String sql, Function<RowResult, T> builder);

    /**
     * Runs a SQL statement against the BigQuery warehouse.
     * @param namedParameters the named parameters for the SQL statement
     * @param sql the SQL statement, must contain <code>@</code> as named parameter prefix
     */
    void execute(Map<String, ?> namedParameters, String sql);

    /**
     * Run a SQL query against the BigQuery warehouse and map the results into an single object if present.
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the function mapping the result into an object
     * @param <T> type of the result objects
     * @return the optional holding the first returned result or an empty optinal
     */
    default <T> Optional<T> querySingle(String sql, Function<RowResult, T> builder) {
        return querySingle(Collections.emptyMap(), sql, builder);
    }

    /**
     * Run a SQL query against the BigQuery warehouse and map the results into an single object if present.
     * @param namedParameters the named parameters for the SQL query
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the function mapping the result into an object
     * @param <T> type of the result objects
     * @return the optional holding the first returned result or an empty optinal
     */
    default <T> Optional<T> querySingle(Map<String, ?> namedParameters, String sql, Function<RowResult, T> builder) {
        return Optional.ofNullable(query(namedParameters, sql, builder).blockingFirst(null));
    }

    /**
     * Run a SQL query against the BigQuery warehouse and map the results into an object.
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the function mapping the result into an object
     * @param <T> type of the result objects
     * @return the flowable of objects mapped using the builder
     */
    default <T> Flowable<T> query(String sql, Function<RowResult, T> builder) {
        return query(Collections.emptyMap(), sql, builder);
    }

    /**
     * Runs a SQL statement against the BigQuery warehouse
     * @param sql the SQL statement, must contain <code>@</code> as named parameter prefix
     */
    default void execute(String sql) {
        execute(Collections.emptyMap(), sql);
    }

    /**
     * Inserts the object into the database.
     *
     * @param object the object to be inserted
     * @param dataset the name of the dataset
     * @param table the name of the table
     * @param <T> the type of the inserted object
     * @return the very same object as has been passed into this method
     */
    default <T> T insert(T object, String dataset, String table) {
        ParameterizedSql insert = generateInsert(object, dataset, table);
        execute(insert.getNamedParameters(), insert.getSql());
        return object;
    }

    /**
     * Generates the insert statement with the named parameters prepared.
     *
     * @param object the object to be inserted
     * @param dataset the name of the dataset
     * @param table the name of the table
     * @param <T> the type of the inserted object
     * @return the sql and named parameters for the insertion of the given object
     */
    @SuppressWarnings("unchecked")
    default <T> ParameterizedSql generateInsert(T object, final String dataset, String table) {
        BeanIntrospection<T> introspection = BeanIntrospector.SHARED.getIntrospection((Class<T>) object.getClass());

        Collection<BeanProperty<T, Object>> fields = introspection.getBeanProperties();

        final List<String> keys = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();
        for (BeanProperty<T, Object> field : fields) {
            Object value = field.get(object);
            String formattedName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());

            if (value != null) {
                keys.add(formattedName);
                values.put(formattedName, convertIfNecessary(value));
            }
        }

        String builder = String.format(
                "insert into %s.%s (%s) values (%s)",
                dataset,
                table,
                String.join(", ", keys),
                keys.stream().map(k -> "@" + k).collect(Collectors.joining(", "))
        );

        return ParameterizedSql.from(values, builder);
    }

    /**
     * Converts the object value to the value suitable for the underlying database if necessary.
     * @param object the object which might need conversion
     * @return either the original object or a representation of the object which is suitable for the database
     */
    default Object convertIfNecessary(Object object) {
        if (object instanceof Enum) {
            return object.toString();
        }

        return object;
    }

}
