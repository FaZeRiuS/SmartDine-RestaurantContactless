package com.example.CourseWork.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SecurityProperties {

    /**
     * Comma-separated allowed origin patterns for Spring CORS ({@link SecurityConfig}, {@link WebConfig}).
     * Used for cross-origin HTTP (e.g. API from another host/port), not specific to SSE.
     */
    @Value("${app.cors.allowed-origins:http://localhost:8081}")
    private String corsAllowedOrigins;

    public List<String> getCorsAllowedOrigins() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}

