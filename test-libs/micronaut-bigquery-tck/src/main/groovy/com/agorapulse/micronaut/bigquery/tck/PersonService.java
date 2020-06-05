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
package com.agorapulse.micronaut.bigquery.tck;

import com.agorapulse.micronaut.bigquery.BigQueryService;
import com.agorapulse.micronaut.bigquery.RowResult;
import io.micronaut.context.annotation.Value;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Optional;

@Singleton
public class PersonService {

    private final String schema;
    private final String table;
    private final BigQueryService bq;

    public PersonService(
        @Value("${person.schema:persons}") String schema,
        @Value("${person.table:persons}") String table,
        BigQueryService bq
    ) {
        this.schema = schema;
        this.table = table;
        this.bq = bq;
    }

    public Person createPerson(String firstName, String lastName, String email, Role role) {
        Person person = new Person();
        person.setId(System.currentTimeMillis());
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setEmail(email);
        person.setRole(role);

        return bq.insert(person, schema, table);
    }

    public Optional<Person> get(long id) {
        return bq.querySingle(
            Collections.singletonMap("id", id),
            String.format("select * from %s.%s where id = %sid", schema, table, bq.getVariablePrefix()),
            PersonService::buildPerson
        );
    }

    public Optional<Person> getUnsafe(long id) {
        return bq.querySingle(
            String.format("select * from %s.%s where id = %d", schema, table, id),
            PersonService::buildPerson
        );
    }

    public Flowable<Person> findByLastNameUnsafe(String lastName) {
        return bq.query(
            String.format("select * from %s.%s where last_name = '%s'", schema, table, lastName),
            PersonService::buildPerson
        );
    }

    public Flowable<Person> findByLastName(String lastName) {
        return bq.query(
            Collections.singletonMap("last_name", lastName),
            String.format("select * from %s.%s where last_name = %slast_name", schema, table, bq.getVariablePrefix()),
            PersonService::buildPerson
        );
    }

    public void deletePerson(long id) {
        bq.execute(
            Collections.singletonMap("id", id),
            String.format("delete from %s.%s where id = %sid", schema, table, bq.getVariablePrefix())
        );
    }

    public void deleteEverything() {
        bq.execute(
            String.format("delete from %s.%s where 1 = 1", schema, table)
        );
    }

    private static Person buildPerson(RowResult results) {
        Person person = new Person();
        person.setId(results.getLongValue("id"));
        person.setFirstName(results.getStringValue("first_name"));
        person.setLastName(results.getStringValue("last_name"));
        person.setEmail(results.getStringValue("email"));
        person.setRole(results.getEnumValue("role", Role.class));
        person.setScore(results.getDoubleValue("score"));
        person.setCreated(results.getTimestampValue("created"));
        person.setEnabled(results.getBooleanValue("enabled"));
        return person;
    }
}
