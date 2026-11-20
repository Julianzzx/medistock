package org.db.medistock.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@Conditional(OAuth2CredentialsCondition.class)
public class OAuth2ClientConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();

        String googleClientId = environment.getProperty("app.oauth2.google.client-id", "").trim();
        String googleClientSecret = environment.getProperty("app.oauth2.google.client-secret", "").trim();
        if (!googleClientId.isEmpty() && !googleClientSecret.isEmpty()) {
            String googleScope = environment.getProperty("app.oauth2.google.scope", "profile,email");
            String googleRedirectUri = environment.getProperty("app.oauth2.google.redirect-uri",
                    "{baseUrl}/login/oauth2/code/{registrationId}");
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .scope(googleScope.split(","))
                    .redirectUri(googleRedirectUri)
                    .clientName("Google")
                    .build());
        }

        String githubClientId = environment.getProperty("app.oauth2.github.client-id", "").trim();
        String githubClientSecret = environment.getProperty("app.oauth2.github.client-secret", "").trim();
        if (!githubClientId.isEmpty() && !githubClientSecret.isEmpty()) {
            String githubScope = environment.getProperty("app.oauth2.github.scope", "read:user,user:email");
            String githubRedirectUri = environment.getProperty("app.oauth2.github.redirect-uri",
                    "{baseUrl}/login/oauth2/code/{registrationId}");
            registrations.add(ClientRegistration.withRegistrationId("github")
                    .clientId(githubClientId)
                    .clientSecret(githubClientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(githubRedirectUri)
                    .scope(githubScope.split(","))
                    .authorizationUri("https://github.com/login/oauth/authorize")
                    .tokenUri("https://github.com/login/oauth/access_token")
                    .userInfoUri("https://api.github.com/user")
                    .userNameAttributeName("login")
                    .clientName("GitHub")
                    .build());
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }
}
