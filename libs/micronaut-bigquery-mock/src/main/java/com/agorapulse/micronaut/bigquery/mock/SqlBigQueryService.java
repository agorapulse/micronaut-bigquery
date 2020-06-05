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
package com.agorapulse.micronaut.bigquery.mock;

import com.agorapulse.micronaut.bigquery.BigQueryService;
import com.agorapulse.micronaut.bigquery.DefaultBigQueryService;
import com.agorapulse.micronaut.bigquery.RowResult;
import com.axiomalaska.jdbc.NamedParameterPreparedStatement;
import io.micronaut.context.annotation.Replaces;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@Singleton
@Replaces(DefaultBigQueryService.class)
public class SqlBigQueryService implements BigQueryService {

    private static class Database {
        private final Connection connection;
        private final PreparedStatement statement;
        private final ResultSet resultSet;

        public Database(Connection connection, PreparedStatement statement, ResultSet resultSet) {
            this.connection = connection;
            this.statement = statement;
            this.resultSet = resultSet;
        }
    }

    private final DataSource dataSource;

    public SqlBigQueryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> Flowable<T> query(Map<String, Object> namedParameters, String sqlString, final Function<RowResult, T> builder) {

        return Flowable.generate(
            () -> {
                Connection connection = dataSource.getConnection();
                NamedParameterPreparedStatement stmt = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(connection, sqlString);
                fillNamedParameters(namedParameters, stmt);
                return new Database(connection, stmt, stmt.executeQuery());
            },
            (database, emitter) -> {
                try {
                    SqlRowResult rowResult = new SqlRowResult(database.resultSet);
                    if (database.resultSet.next()) {
                        emitter.onNext(builder.apply(rowResult));
                    } else {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
                return database;
            },
            database -> {
                database.resultSet.close();
                database.statement.close();
                database.connection.close();
            }
        );
    }

    @Override
    public void execute(Map<String, Object> namedParameters, String sqlString) {
        try (
            Connection connection = dataSource.getConnection();
            NamedParameterPreparedStatement stmt = NamedParameterPreparedStatement.createNamedParameterPreparedStatement(connection, sqlString)
        ) {
            fillNamedParameters(namedParameters, stmt);
            stmt.execute();
        } catch (SQLException e) {
            throw new IllegalArgumentException("Cannot execute " + sqlString, e);
        }
    }

    @Override
    public String getVariablePrefix() {
        return ":";
    }

    private void fillNamedParameters(Map<String, Object> namedParameters, NamedParameterPreparedStatement stmt) {
        namedParameters.forEach((parameter, x) -> {
            try {
                stmt.setObject(parameter, convertIfNecessary(x));
            } catch (SQLException throwables) {
                throw new IllegalStateException("Cannot set named parameter " + parameter + " with value " + x, throwables);
            }
        });
    }

    @Override
    public Object convertIfNecessary(Object object) {
        if (object instanceof Instant) {
            return new Timestamp(((Instant) object).toEpochMilli());
        }
        return BigQueryService.super.convertIfNecessary(object);
    }
}
