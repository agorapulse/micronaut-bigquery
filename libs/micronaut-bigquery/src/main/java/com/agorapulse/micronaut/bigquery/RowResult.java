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

import java.time.Instant;

/**
 * Row result is the abstraction of a row returned from the BigQuery job.
 */
public interface RowResult {

    /**
     * Returns <code>true</code> if the column value returned is null.
     * @param key the name of the column
     * @return <code>true</code> if the column value returned is null
     */
    boolean isNull(String key);

    /**
     * Returns the boolean value of the column if applicable.
     * @param key the name of the column
     * @return the boolean value of the column if applicable
     */
    Boolean getBooleanValue(String key);

    /**
     * Returns the double value of the column if applicable.
     * @param key the name of the column
     * @return the double value of the column if applicable
     */
    Double getDoubleValue(String key);

    /**
     * Returns the string value of the column if applicable.
     * @param key the name of the column
     * @return the string value of the column if applicable
     */
    String getStringValue(String key);

    /**
     * Returns the long value of the column if applicable.
     * @param key the name of the column
     * @return the long value of the column if applicable
     */
    Long getLongValue(String key);

    /**
     * Returns the value of the column as {@link Instant} if applicable.
     * @param key the name of the column
     * @return the value of the column as {@link Instant} if applicable
     */
    Instant getTimestampValue(String key);

    /**
     * Returns the enum value of the column if applicable.
     * @param key the name of the column
     * @param enumType the enum type
     * @param <E> the enum type
     * @return the enum value of the column if applicable
     */
    default <E extends Enum<E>> E getEnumValue(String key, Class<E> enumType) {
        return isNull(key) ? null : Enum.valueOf(enumType, getStringValue(key));
    }

}
