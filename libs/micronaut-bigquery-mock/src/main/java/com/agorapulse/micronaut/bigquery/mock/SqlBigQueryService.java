/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.bigquery.mock;

import com.agorapulse.micronaut.bigquery.BigQueryService;
import com.agorapulse.micronaut.bigquery.DefaultBigQueryService;
import com.agorapulse.micronaut.bigquery.RowResult;
import groovy.lang.Closure;
import groovy.sql.Sql;
import io.micronaut.context.annotation.Replaces;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

@Singleton
@Replaces(DefaultBigQueryService.class)
public class SqlBigQueryService implements BigQueryService {

    private final DataSource dataSource;

    public SqlBigQueryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> Flowable<T> query(Map<String, Object> namedParameters, String sqlString, final Function<RowResult, T> builder) {

        return Flowable.generate(emitter -> {
            Sql sql = new Sql(dataSource);
            sql.query(namedParameters, sqlString, new Closure<Void>(this, this) {
                @Override
                public Void call(Object... args) {
                    ResultSet resultSet = (ResultSet) args[0];
                    try {
                        SqlRowResult rowResult = new SqlRowResult(resultSet);
                        while (resultSet.next()) {
                            emitter.onNext(builder.apply(rowResult));
                        }
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                    return null;
                }
            });
        });

    }

    @Override
    public void execute(Map<String, Object> namedParameters, String sqlString) {
        try {
            Sql sql = new Sql(dataSource);
            sql.execute(namedParameters, sqlString);
        } catch (SQLException throwables) {
            throw new IllegalArgumentException("Cannot " + sqlString, throwables);
        }
    }

    @Override
    public String getVariablePrefix() {
        return ":";
    }

}
