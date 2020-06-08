/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Agorapulse.
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
package com.agorapulse.micronaut.bigquery.groovy;

import com.agorapulse.micronaut.bigquery.BigQueryService;
import com.agorapulse.micronaut.bigquery.ParameterizedSql;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import io.reactivex.Flowable;
import space.jasan.support.groovy.closure.FunctionWithDelegate;

import java.util.*;

public class MicronautBigQueryExtensions {

    private static final List<String> PLACEHOLDERS_ALLOWED_AFTER_KEYWORD = Arrays.asList(
        " where ", " on ", " set ", " values "
    );

    /**
     * Runs a SQL query against the BigQuery warehouse and map the results into an object.
     * @param sql the SQL query {@link GString} which is automatically turning its values into the named parameters
     * @param builder the closure mapping the result into an object
     * @param <T> type of the result objects
     * @return the flowable of objects mapped using the builder
     */
    public static <T> Flowable<T> query(
        BigQueryService self,
        GString sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        ParameterizedSql parameterizedSql = from(self, sql);
        return self.query(parameterizedSql.getNamedParameters(), parameterizedSql.getSql(), FunctionWithDelegate.create(builder));
    }

    /**
     * Runs a SQL query against the BigQuery warehouse and map the results into an object.
     * @param namedParameters the named parameters for the SQL query
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the closure mapping the result into an object
     * @param <T> type of the result objects
     * @return the flowable of objects mapped using the builder
     */
    public static <T> Flowable<T> query(
        BigQueryService self,
        Map<String, ?> namedParameters,
        String sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        return self.query(namedParameters, sql, FunctionWithDelegate.create(builder));
    }

    /**
     * Run a SQL query against the BigQuery warehouse and map the results into an single object if present.
     * @param sql the SQL query {@link GString} which is automatically turning its values into the named parameters
     * @param builder the closure mapping the result into an object
     * @param <T> type of the result objects
     * @return the optional holding the first returned result or an empty optinal
     */
    public static <T> Optional<T> querySingle(
        BigQueryService self,
        GString sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        ParameterizedSql parameterizedSql = from(self, sql);
        return self.querySingle(parameterizedSql.getNamedParameters(), parameterizedSql.getSql(), FunctionWithDelegate.create(builder));
    }

    /**
     * Run a SQL query against the BigQuery warehouse and map the results into an single object if present.
     * @param namedParameters the named parameters for the SQL query
     * @param sql the SQL query, must contain <code>@</code> as named parameter prefix
     * @param builder the closure mapping the result into an object
     * @param <T> type of the result objects
     * @return the optional holding the first returned result or an empty optinal
     */
    public static <T> Optional<T> querySingle(
        BigQueryService self,
        Map<String, ?> namedParameters,
        String sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        return self.querySingle(namedParameters, sql, FunctionWithDelegate.create(builder));
    }

    /**
     * Runs a SQL statement against the BigQuery warehouse.
     * @param sql the SQL query {@link GString} which is automatically turning its values into the named parameters
     */
    public static void execute(BigQueryService self, GString sql) {
        ParameterizedSql preparedSql = from(self, sql);
        self.execute(preparedSql.getNamedParameters(), preparedSql.getSql());
    }

    private static ParameterizedSql from(BigQueryService service, GString gString) {
        StringBuilder builder = new StringBuilder();
        Map<String, Object> namedParameters = new LinkedHashMap<>();
        for (int i = 0; i < gString.getStrings().length; i++){
            builder.append(gString.getStrings()[i]);
            String current = builder.toString().toLowerCase();
            if (i != gString.getStrings().length - 1) {
                if (PLACEHOLDERS_ALLOWED_AFTER_KEYWORD.stream().anyMatch(current::contains)) {
                    String varName = "var" + i;
                    builder.append("@").append(varName);
                    namedParameters.put(varName, service.convertIfNecessary(gString.getValues()[i]));
                } else {
                    builder.append(gString.getValues()[i]);
                }
            }
        }

        return ParameterizedSql.from(namedParameters, builder.toString());
    }

}
