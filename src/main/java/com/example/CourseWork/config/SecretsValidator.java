package com.example.CourseWork.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SecretsValidator {

    private final Environment environment;

    @PostConstruct
    public void validateSecrets() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        String keycloakSecret = environment.getProperty("spring.security.oauth2.client.registration.keycloak.client-secret");
        String liqpayPublicKey = environment.getProperty("liqpay.public.key");
        String liqpayPrivateKey = environment.getProperty("liqpay.private.key");
        String hmacSecret = environment.getProperty("app.security.qr.hmac-secret");

        if (isInvalid(keycloakSecret)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: KEYCLOAK_CLIENT_SECRET is missing or empty.");
        }
        if (isInvalid(liqpayPublicKey)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: LIQPAY_PUBLIC_KEY is missing or empty.");
        }
        if (isInvalid(liqpayPrivateKey)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: LIQPAY_PRIVATE_KEY is missing or empty.");
        }
        if (isInvalid(hmacSecret) || "default-unsafe-secret".equals(hmacSecret)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: APP_HMAC_SECRET is missing, empty, or insecure.");
        }
    }

    private boolean isInvalid(String val) {
        return val == null || val.isBlank() || "change_me".equalsIgnoreCase(val.trim());
    }
}
