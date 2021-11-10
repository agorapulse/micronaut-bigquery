/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 Agorapulse.
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
package com.agorapulse.micronaut.bigquery

import com.agorapulse.micronaut.bigquery.tck.BigQueryServiceSpec
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import io.micronaut.context.ApplicationContext
import spock.lang.Requires

@SuppressWarnings('ClassStartsWithBlankLine')
@Requires({
    System.getenv('GOOGLE_APPLICATION_CREDENTIALS')
})
class DefaultBigQueryServiceSpec extends BigQueryServiceSpec {

    private static final BigQuery BG = BigQueryOptions.defaultInstance.service
    private static final String DATASET_NAME = 'persons'
    private static final String TABLE_NAME = DATASET_NAME + System.currentTimeMillis()

    void setupSpec() {
        TableId tableId = TableId.of(DATASET_NAME, TABLE_NAME)

        // Table schema definition
        Schema schema = Schema.of(
            Field.of('id', LegacySQLTypeName.INTEGER),
            Field.of('enabled', LegacySQLTypeName.BOOLEAN),
            Field.of('created', LegacySQLTypeName.TIMESTAMP),
            Field.of('score', LegacySQLTypeName.FLOAT),
            Field.of('first_name', LegacySQLTypeName.STRING),
            Field.of('last_name', LegacySQLTypeName.STRING),
            Field.of('email', LegacySQLTypeName.STRING),
            Field.of('role', LegacySQLTypeName.STRING)
        )
        TableDefinition tableDefinition = StandardTableDefinition.of(schema)
        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        BG.create(tableInfo)
    }

    void cleanupSpec() {
        BG.delete(TableId.of(DATASET_NAME, TABLE_NAME))
    }

    @Override
    ApplicationContext buildContext() {
        return ApplicationContext.build(
            'person.schema': DATASET_NAME,
            'person.table': TABLE_NAME
        ).build()
    }

}
