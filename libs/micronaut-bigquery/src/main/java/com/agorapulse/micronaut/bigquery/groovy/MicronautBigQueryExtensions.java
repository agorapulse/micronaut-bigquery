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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MicronautBigQueryExtensions {

    public static <T> Flowable<T> query(
        BigQueryService self,
        GString gString,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        ParameterizedSql sql = from(self, gString);
        return self.query(sql.getNamedParameters(), sql.getSql(), FunctionWithDelegate.create(builder));
    }

    public static <T> Flowable<T> query(
        BigQueryService self,
        Map<String, Object> namedParameters,
        String sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        return self.query(namedParameters, sql, FunctionWithDelegate.create(builder));
    }

    public static <T> Optional<T> querySingle(
        BigQueryService self,
        GString gString,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        ParameterizedSql sql = from(self, gString);
        return self.querySingle(sql.getNamedParameters(), sql.getSql(), FunctionWithDelegate.create(builder));
    }

    public static <T> Optional<T> querySingle(
        BigQueryService self,
        Map<String, Object> namedParameters,
        String sql,
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.bigquery.RowResult") Closure<T> builder
    ) {
        return self.querySingle(namedParameters, sql, FunctionWithDelegate.create(builder));
    }

    public static void write(BigQueryService self, GString gString) {
        ParameterizedSql sql = from(self, gString);
        self.execute(sql.getNamedParameters(), sql.getSql());
    }

    private static ParameterizedSql from(BigQueryService service, GString gString) {
        StringBuilder builder = new StringBuilder();
        Map<String, Object> namedParameters = new LinkedHashMap<>();
        for (int i = 0; i < gString.getStrings().length; i++){
            builder.append(gString.getStrings()[i]);
            if (i != gString.getStrings().length - 1) {
                String varName = "var" + i;
                builder.append(service.getVariablePrefix()).append(varName);
                namedParameters.put(varName, service.convertIfNecessary(gString.getValues()[i]));
            }
        }

        return ParameterizedSql.from(namedParameters, builder.toString());
    }

}
