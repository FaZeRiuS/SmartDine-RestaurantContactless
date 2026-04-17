package com.example.CourseWork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Tests run without Keycloak; default OAuth2 client auto-config calls the issuer well-known
 * endpoint at startup. This stub supplies a static {@link ClientRegistration} so
 * {@code @ConditionalOnMissingBean(ClientRegistrationRepository.class)} skips discovery.
 */
@Configuration
@Profile("test")
public class OAuth2ClientTestStubConfig {

    private static final String DUMMY = "http://127.0.0.1:1";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration keycloak = ClientRegistration.withRegistrationId("keycloak")
                .clientId("restaurant-client")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(DUMMY + "/realms/restaurant-realm/protocol/openid-connect/auth")
                .tokenUri(DUMMY + "/realms/restaurant-realm/protocol/openid-connect/token")
                .userInfoUri(DUMMY + "/realms/restaurant-realm/protocol/openid-connect/userinfo")
                .jwkSetUri(DUMMY + "/realms/restaurant-realm/protocol/openid-connect/certs")
                .issuerUri(DUMMY + "/realms/restaurant-realm")
                .userNameAttributeName("preferred_username")
                .clientName("Keycloak")
                .build();
        return new InMemoryClientRegistrationRepository(keycloak);
    }
}
