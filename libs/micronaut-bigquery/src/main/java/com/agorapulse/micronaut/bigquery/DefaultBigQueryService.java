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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import io.reactivex.Flowable;

import javax.inject.Singleton;
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
    public <T> Flowable<T> query(Map<String, Object> namedParameters, String sql, Function<RowResult, T> builder) {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
            .newBuilder(sql)
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
            return Flowable.fromIterable(result.iterateAll()).map(FieldValueListRowResult::new).map(builder::apply);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting for the result has been interrupted!", e);
        }
    }

    @Override
    public void execute(Map<String, Object> namedParameters, String sql) {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
            .newBuilder(sql)
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
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting for the result has been interrupted!", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, QueryParameterValue> toNamedParameters(Map<String, Object> namedParameters) {
        if (namedParameters.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, QueryParameterValue> result = new LinkedHashMap<>();

        namedParameters.forEach((key, value) -> {
                result.put(key, QueryParameterValue.of(value, (Class<Object>) value.getClass()));
        });

        return result;
    }

}
