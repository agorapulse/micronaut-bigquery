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

import com.agorapulse.micronaut.bigquery.RowResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class SqlRowResult implements RowResult {

    private final ResultSet result;

    public SqlRowResult(ResultSet result) {
        this.result = result;
    }

    @Override
    public boolean isNull(String key) {
        try {
            return result.getObject(key) != null;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read object value", e);
        }
    }

    @Override
    public Boolean getBooleanValue(String key) {
        try {
            return result.getBoolean(key);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read boolean value", e);
        }
    }

    @Override
    public Double getDoubleValue(String key) {
        try {
            return result.getDouble(key);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read double value", e);
        }
    }

    @Override
    public String getStringValue(String key) {
        try {
            return result.getString(key);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read string value", e);
        }
    }

    @Override
    public Long getLongValue(String key) {
        try {
            return result.getLong(key);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read long value", e);
        }
    }

    @Override
    public Instant getTimestampValue(String key) {
        try {
            return Optional.ofNullable(result.getDate(key)).map(date -> Instant.ofEpochMilli(date.getTime())).orElse(null);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read timestamp value", e);
        }
    }

}
