package com.agorapulse.micronaut.bigquery

import com.agorapulse.micronaut.bigquery.tck.BigQueryServiceSpec
import io.micronaut.context.ApplicationContext
import spock.lang.Requires

@Requires({
    System.getenv('GOOGLE_APPLICATION_CREDENTIALS') &&
        (
            !System.getenv('GITHUB_REF') ||
            (
                System.getenv('GITHUB_REF') && System.getenv('GITHUB_REF').startsWith('refs/heads/')
            )
        )
})
class DefaultBigQueryServiceSpec extends BigQueryServiceSpec {

    @Override
    ApplicationContext buildContext() {
        ApplicationContext.build().build()
    }

}
