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
package com.agorapulse.micronaut.bigquery.tck

import com.agorapulse.micronaut.bigquery.BigQueryService
import com.agorapulse.micronaut.bigquery.RowResult
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import io.reactivex.Flowable

import javax.inject.Singleton

@Singleton
@CompileStatic
class GroovyPersonService implements PersonService {

    private final String schema
    private final String table
    private final BigQueryService bq

    GroovyPersonService(
        @Value('${person.schema:persons}') String schema,
        @Value('${person.table:persons}') String table,
        BigQueryService bq
    ) {
        this.schema = schema
        this.table = table
        this.bq = bq
    }

    @Override
    Person createPerson(String firstName, String lastName, String email, Role role) {
        return bq.insert(new Person(
            id: System.currentTimeMillis(),
            firstName: firstName,
            lastName: lastName,
            role: role
        ), schema, table)
    }

    @Override
    Optional<Person> get(long id) {
        return bq.querySingle("select * from ${schema}.${table} where id = $id") {
            return buildPerson(it)
        }
    }

    @Override
    Optional<Person> getUnsafe(long id) {
        return bq.querySingle("select * from ${schema}.${table} where id = @id", id: id) {
            return buildPerson(it)
        }
    }

    @Override
    Flowable<Person> findByLastNameUnsafe(String lastName) {
        return bq.query("select * from ${schema}.${table} where last_name = $lastName") {
            return buildPerson(it)
        }
    }

    @Override
    Flowable<Person> findByLastName(String lastName) {
        return bq.query("select * from ${schema}.${table} where last_name = @lastName", lastName: lastName) {
            return buildPerson(it)
        }
    }

    @Override
    void updateRole(long id, Role role) {
        bq.execute "update ${schema}.${table} set role = $role where id = $id"
    }

    @Override
    void deletePerson(long id) {
        bq.execute "delete from ${schema}.${table} where id = $id"
    }

    @Override
    void deleteEverything() {
        bq.execute"delete from ${schema}.${table} where 1 = 1"
    }

    private static Person buildPerson(RowResult result) {
        new Person(
            id: result.getLongValue('id'),
            firstName: result.getStringValue('first_name'),
            lastName: result.getStringValue('last_name'),
            email: result.getStringValue('email'),
            role: result.getEnumValue('role', Role),
            score: result.getDoubleValue('score'),
            created: result.getTimestampValue('created'),
            enabled: result.getBooleanValue('enabled')
        )
    }

}
