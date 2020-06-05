package com.agorapulse.micronaut.bigquery

import com.agorapulse.micronaut.bigquery.tck.BigQueryServiceSpec
import io.micronaut.context.ApplicationContext

class DefaultBigQueryServiceSpec extends BigQueryServiceSpec {

    @Override
    ApplicationContext buildContext() {
        ApplicationContext.build().build()
    }

}
