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
package com.agorapulse.micronaut.bigquery;

import java.util.Map;

/**
 * Parametrized SQL contains the statement and the map of the named parameters.
 */
public final class ParameterizedSql {

    /**
     * Creates the named parameters from the map of named parameters and a SQL statement
     * @param namedParameters the map of the named parameters
     * @param sql the sql statement, must contain <code>@</code> as named parameter prefix
     * @return
     */
    public static ParameterizedSql from(Map<String, ?> namedParameters, String sql) {
        return new ParameterizedSql(namedParameters, sql);
    }

    private ParameterizedSql(Map<String, ?> namedParameters, String sql) {
        this.namedParameters = namedParameters;
        this.sql = sql;
    }

    /**
     * @return the map of the named parameters
     */
    public Map<String, ?> getNamedParameters() {
        return namedParameters;
    }

    /**
     * @return the SQL statement
     */
    public String getSql() {
        return sql;
    }

    private final Map<String, ?> namedParameters;
    private final String sql;
}
