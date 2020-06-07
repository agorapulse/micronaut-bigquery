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
package com.agorapulse.micronaut.bigquery.mock

import com.agorapulse.micronaut.bigquery.tck.BigQueryServiceSpec
import groovy.sql.Sql
import groovy.transform.CompileDynamic
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

@CompileDynamic
@Testcontainers
class SqlBigQueryServiceSpec extends BigQueryServiceSpec {

    // language=SQL
    // tag::test-setup[]
    private static final String TABLE_DEFINITION = '''
    CREATE SCHEMA persons;
    CREATE TABLE persons.persons (
        id bigserial primary key,
        first_name character varying(256),
        last_name character varying(256),
        email character varying(256),
        role character varying(10),
        score numeric,
        enabled boolean,
        created timestamp
    );
    '''

    private static final String DRIVER = 'org.postgresql.Driver'

    @Shared PostgreSQLContainer container = new PostgreSQLContainer()                   // <1>
    // end::test-setup[]

    // tag::setup-spec[]
    void setupSpec() {
        Sql sql = Sql.newInstance(
            container.jdbcUrl,
            container.username,
            container.password,
            DRIVER
        )
        sql.execute(TABLE_DEFINITION)                                                   // <2>
    }
    // end::setup-spec[]

    @Override
    ApplicationContext buildContext() {
        return ApplicationContext.build(
            // tag::build-context[]
            'datasources.default.url': container.jdbcUrl,
            'datasources.default.driverClassName': DRIVER,
            'datasources.default.username': container.username,
            'datasources.default.password': container.password,
            // end::build-context[]
        ).build()
    }

}
