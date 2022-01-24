/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 Agorapulse.
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
package com.agorapulse.micronaut.bigquery.impl;

import com.agorapulse.micronaut.bigquery.BigQueryService;
import com.agorapulse.micronaut.bigquery.RowResult;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Singleton
public class DefaultBigQueryService implements BigQueryService {

    private final BigQuery bigquery;

    public DefaultBigQueryService(BigQuery bigQuery) {
        this.bigquery = bigQuery;
    }

    @Override
    public <T> Flowable<T> query(Map<String, ?> namedParameters, String sql, Function<RowResult, T> builder) {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
            .newBuilder(checkForNulls(sql, namedParameters))
            .setUseLegacySql(false)
            .setNamedParameters(toNamedParameters(namedParameters))
            .build();

        // Create a job ID
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        try {
            // Wait for the query to complete.
            Job completedJob = job.waitFor();

            // Check for errors
            if (completedJob == null) {
                throw new IllegalStateException("Job no longer exists");
            } else if (completedJob.getStatus().getError() != null) {
                throw new IllegalStateException("Failed to execute sql " + sql + ":" + completedJob.getStatus().getError());
            }


            TableResult result = completedJob.getQueryResults();
            return Flowable.fromIterable(result.iterateAll()).filter(r -> !r.isEmpty()).map(FieldValueListRowResult::new).map(builder::apply);
        } catch (InterruptedException | BigQueryException e) {
            throw new IllegalStateException("Could not execute query: " + sql, e);
        }
    }

    @Override
    public void execute(Map<String, ?> namedParameters, String sql) {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
            .newBuilder(checkForNulls(sql, namedParameters))
            .setUseLegacySql(false)
            .setNamedParameters(toNamedParameters(namedParameters))
            .build();

        // Create a job ID
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        try {
            // Wait for the query to complete.
            Job completedJob = job.waitFor();

            // Check for errors
            if (completedJob == null) {
                throw new IllegalStateException("Job no longer exists");
            } else if (completedJob.getStatus().getError() != null) {
                throw new IllegalStateException(completedJob.getStatus().getError().toString());
            }
        } catch (InterruptedException | BigQueryException e) {
            throw new IllegalStateException("Could not execute sql: " + sql, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, QueryParameterValue> toNamedParameters(Map<String, ?> namedParameters) {
        if (namedParameters.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, QueryParameterValue> result = new LinkedHashMap<>();

        namedParameters.forEach((key, value) -> {
            Object converted = convertIfNecessary(value);
            if (converted instanceof Instant) {
                Instant instant = (Instant) converted;
                result.put(key, QueryParameterValue.timestamp(instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000));
            } else if (converted != null){
                result.put(key, QueryParameterValue.of(converted, (Class<Object>) converted.getClass()));
            }
        });

        return result;
    }

    private String checkForNulls(String sql, Map<String, ?> namedParameters) {
        String result = sql;
        for(Map.Entry<String, ?> entry : namedParameters.entrySet()) {
            if (entry.getValue() == null) {
                result = result.replace("@" + entry.getKey(), "null");
            }
        }
        return result;
    }

}
