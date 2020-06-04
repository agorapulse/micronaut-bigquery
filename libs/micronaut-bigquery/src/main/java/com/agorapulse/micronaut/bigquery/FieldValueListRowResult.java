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
package com.agorapulse.micronaut.bigquery;

import com.google.cloud.bigquery.FieldValueList;

import java.time.Instant;

class FieldValueListRowResult implements RowResult {

    private final FieldValueList values;

    FieldValueListRowResult(FieldValueList values) {
        this.values = values;
    }

    @Override
    public boolean isNull(String key) {
        return values.get(key).isNull();
    }

    @Override
    public Boolean getBooleanValue(String key) {
        return values.get(key).isNull() ? values.get(key).getBooleanValue() : null;
    }

    @Override
    public Double getDoubleValue(String key) {
        return values.get(key).isNull() ? values.get(key).getDoubleValue() : null;
    }

    @Override
    public String getStringValue(String key) {
        return values.get(key).isNull() ? values.get(key).getStringValue() : null;
    }

    @Override
    public Long getLongValue(String key) {
        return values.get(key).isNull() ? values.get(key).getLongValue() : null;
    }

    @Override
    public Instant getTimestampValue(String key) {
        return values.get(key).isNull() ? Instant.ofEpochMilli(values.get(key).getTimestampValue() / 1000) : null;
    }

}
