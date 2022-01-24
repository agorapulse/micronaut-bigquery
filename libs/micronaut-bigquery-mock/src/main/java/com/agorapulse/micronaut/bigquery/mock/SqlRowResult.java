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
package com.agorapulse.micronaut.bigquery.mock;

import com.agorapulse.micronaut.bigquery.RowResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class SqlRowResult implements RowResult {

    @FunctionalInterface
    private interface Extractor<R> {

        R extract(String key) throws SQLException;

    }

    private final ResultSet result;

    public SqlRowResult(ResultSet result) {
        this.result = result;
    }

    @Override
    public boolean isNull(String key) {
        return getValue(key, k -> result.getObject(key) == null);
    }

    @Override
    public Boolean getBooleanValue(String key) {
        return getValue(key, result::getBoolean);
    }

    @Override
    public Double getDoubleValue(String key) {
        return getValue(key, result::getDouble);
    }

    @Override
    public String getStringValue(String key) {
        return getValue(key, result::getString);
    }

    @Override
    public Long getLongValue(String key) {
        return getValue(key, result::getLong);
    }

    @Override
    public Instant getTimestampValue(String key) {
        return getValue(key, k -> Optional.ofNullable(result.getTimestamp(k)).map(date -> Instant.ofEpochMilli(date.getTime())).orElse(null));
    }

    private <R> R getValue(String key, Extractor<R> extractor) {
        try {
            return extractor.extract(key);
        } catch (SQLException e) {
            throw new IllegalArgumentException("Cannot read value " + key, e);
        }
    }



}
