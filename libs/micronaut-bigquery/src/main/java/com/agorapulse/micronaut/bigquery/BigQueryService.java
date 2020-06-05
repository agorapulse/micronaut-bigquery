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

import com.google.common.base.CaseFormat;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface BigQueryService {

    <T> Flowable<T> query(Map<String, Object> namedParameters, String sql, Function<RowResult, T> builder);
    void execute(Map<String, Object> namedParameters, String sql);

    default String getVariablePrefix() {
        return "@";
    }

    default <T> Optional<T> querySingle(String sql, Function<RowResult, T> builder) {
        return querySingle(Collections.emptyMap(), sql, builder);
    }

    default <T> Optional<T> querySingle(Map<String, Object> namedParameters, String sql, Function<RowResult, T> builder) {
        return Optional.ofNullable(query(namedParameters, sql, builder).blockingFirst(null));
    }

    default <T> Flowable<T> query(String sql, Function<RowResult, T> builder) {
        return query(Collections.emptyMap(), sql, builder);
    }

    default void execute(String sql) {
        execute(Collections.emptyMap(), sql);
    }

    default <T> T insert(T object, String dataset, String table) {
        ParameterizedSql insert = generateInsert(object, dataset, table);
        execute(insert.getNamedParameters(), insert.getSql());
        return object;
    }

    @SuppressWarnings("unchecked")
    default <T> ParameterizedSql generateInsert(T object, final String dataset, String table) {
        BeanIntrospection<T> introspection = BeanIntrospector.SHARED.getIntrospection((Class<T>) object.getClass());

        Collection<BeanProperty<T, Object>> fields = introspection.getBeanProperties();

        final List<String> keys = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();
        for (BeanProperty<T, Object> field : fields) {
            Object value = field.get(object);
            String formattedName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());

            if (value != null) {
                keys.add(formattedName);
                values.put(formattedName, convertIfNecessary(value));
            }
        }

        String builder = String.format(
                "insert into %s.%s (%s) values (%s)",
                dataset,
                table,
                String.join(", ", keys),
                keys.stream().map(k -> getVariablePrefix() + k).collect(Collectors.joining(", "))
        );

        return ParameterizedSql.from(values, builder);
    }

    default Object convertIfNecessary(Object object) {
        if (object instanceof Enum) {
            return object.toString();
        }

        return object;
    }

}
