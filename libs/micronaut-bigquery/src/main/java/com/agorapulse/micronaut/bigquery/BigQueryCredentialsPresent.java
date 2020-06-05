package com.agorapulse.micronaut.bigquery;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.context.env.Environment;

import java.util.Set;

public class BigQueryCredentialsPresent implements Condition {

    @Override
    public boolean matches(ConditionContext context) {
        if (context.getBeanContext() instanceof ApplicationContext) {
            ApplicationContext ctx = (ApplicationContext) context.getBeanContext();
            Set<String> activeNames = ctx.getEnvironment().getActiveNames();
            return activeNames.contains(Environment.GOOGLE_COMPUTE)
                || activeNames.contains(Environment.GOOGLE_COMPUTE)
                || ctx.getProperty("google.application.credentials", String.class).isPresent();
        }
        return false;
    }
}
