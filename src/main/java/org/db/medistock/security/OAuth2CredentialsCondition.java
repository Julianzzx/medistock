package org.db.medistock.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

public final class OAuth2CredentialsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        return hasCredentials(environment, "app.oauth2.google.client-id", "app.oauth2.google.client-secret")
                || hasCredentials(environment, "app.oauth2.github.client-id", "app.oauth2.github.client-secret");
    }

    private boolean hasCredentials(Environment environment, String clientIdKey, String clientSecretKey) {
        return !environment.getProperty(clientIdKey, "").isBlank()
                && !environment.getProperty(clientSecretKey, "").isBlank();
    }
}
