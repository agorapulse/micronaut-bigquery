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
package com.agorapulse.micronaut.bigquery.tck

import com.agorapulse.micronaut.bigquery.BigQueryService
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

@SuppressWarnings([
    'AbstractClassWithoutAbstractMethod',
    'DuplicateStringLiteral',
    'DuplicateNumberLiteral',
])
abstract class BigQueryServiceSpec extends Specification {

    @AutoCleanup ApplicationContext context

    PersonService service
    BigQueryService bigquery

    void setup() {
        context = buildContext()
        context.start()

        service = context.getBean(PersonService)
        bigquery = context.getBean(BigQueryService)
    }

    ApplicationContext buildContext() {
        ApplicationContext.build().build()
    }

    void 'handle bigquery operations'() {
        when:
            Person vlad = service.createPerson('Vladimir', 'Orany', 'vlad@agorapulse.com', Role.ADMIN)
        then:
            noExceptionThrown()

        when:
            Optional<Person> loaded = service.get(vlad.id)
        then:
            loaded.present
            vlad == loaded.get()
            vlad.hashCode() == loaded.get().hashCode()
            !vlad.is(loaded.get())

        when:
            Optional<Person> unsafe = service.getUnsafe(vlad.id)
        then:
            unsafe.present
            vlad == unsafe.get()
            !vlad.is(unsafe.get())

        when:
            Person luke = service.createPerson('Luke', 'Orany', 'luke@awesomeguy.com', Role.GUEST)
        and:
            List<Person> oranys = service.findByLastName('Orany').toList().blockingGet()
            List<Person> oranysUnsafe = service.findByLastNameUnsafe('Orany').toList().blockingGet()
        then:
            oranys.size() == 2
            luke in oranys

            oranysUnsafe.size() == 2
            luke in oranysUnsafe

        when:
            service.deletePerson(vlad.id)
        then:
            !service.get(vlad.id).present
            service.get(luke.id).present

        when:
            service.deleteEverything()
        then:
            !service.get(vlad.id).present
            !service.get(luke.id).present

        when:
            service.findByLastNameUnsafe('\' bobby_tables').blockingFirst()
        then:
            thrown(RuntimeException)

        when:
            Person bobby = service.createPerson('Booby', 'Tables', 'bobby@deletefromtables.com', Role.GUEST)
            bigquery.execute('update persons.persons set role = \'noone\'')
            service.get(bobby.id)
        then:
            thrown(RuntimeException)

        when:
            bigquery.execute('some fake sql with :person', person: bobby)
        then:
            thrown(RuntimeException)

        when:
            bigquery.execute('some fake sql')
        then:
            thrown(RuntimeException)
    }

}
