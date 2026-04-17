package com.example.CourseWork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProperties securityProperties;
    private final String keycloakJwkSetUri;

    public SecurityConfig(
            SecurityProperties securityProperties,
            @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri:http://localhost:8080/realms/restaurant-realm/protocol/openid-connect/certs}") String keycloakJwkSetUri) {
        this.securityProperties = securityProperties;
        this.keycloakJwkSetUri = keycloakJwkSetUri;
    }

    // ─── Single SecurityFilterChain: supports both JWT and OAuth2 Login ───
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // We accept LiqPay callbacks without CSRF (signed by LiqPay).
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/payment/callback"),
                                new AntPathRequestMatcher("/api/notifications/subscribe")))
                .authorizeHttpRequests(auth -> auth
                        // Public static resources & pages
                        .requestMatchers("/", "/menu", "/menu/**", "/cart", "/error",
                                "/css/**", "/js/**", "/images/**", "/static/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/apple-touch-icon.png",
                                "/manifest.json",
                                "/sw.js",
                                "/api/test/auth/**",
                                "/auth/**",
                                "/icons/**",
                                "/uploads/**")
                        .permitAll()
                        // Swagger/OpenAPI endpoints: disabled in prod and blocked here as
                        // defense-in-depth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").denyAll()
                        .requestMatchers("/payment/**").permitAll()
                        // LiqPay callback must be public; payment init is allowed for guests
                        // (session-based GUEST_<sessionId> produced by CurrentUserIdentity)
                        .requestMatchers(HttpMethod.POST, "/api/payment/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payment/init").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payment/checkout").permitAll()
                        // CSRF bootstrap (sets XSRF-TOKEN cookie)
                        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                        // Public API endpoints (GET/POST for Cart and Active Order)
                        .requestMatchers(HttpMethod.GET, "/api/menus/**", "/api/dishes/**").permitAll()
                        .requestMatchers("/api/cart/**").permitAll()
                        .requestMatchers("/htmx/cart/**", "/htmx/orders/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/htmx/menu/**").permitAll()
                        .requestMatchers("/api/user/me").permitAll()
                        // Order API: allow guests (CurrentUserIdentity creates GUEST_<sessionId>)
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers("/api/orders/my-active").permitAll()
                        .requestMatchers("/api/orders/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/items").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/items/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*/items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/pay").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/call-waiter").permitAll()
                        .requestMatchers("/api/notifications/**").permitAll()
                        .requestMatchers("/api/sse/subscribe/**").permitAll()
                        // Staff pages & HTMX staff fragments
                        .requestMatchers("/staff/**", "/htmx/staff/**").hasAnyRole("WAITER", "CHEF", "ADMINISTRATOR")
                        // Admin-only pages
                        .requestMatchers("/admin/dashboard", "/admin/orders", "/admin/qr/**").hasRole("ADMINISTRATOR")
                        // Admin order history HTMX (administrator only; narrower than /htmx/admin/**)
                        .requestMatchers("/htmx/admin/orders/**").hasRole("ADMINISTRATOR")
                        // Menu editor for Chefs and Admins (+ HTMX fragments for that page)
                        .requestMatchers("/admin/menu", "/htmx/admin/**").hasAnyRole("CHEF", "ADMINISTRATOR")
                        // General admin access (fallback)
                        .requestMatchers("/admin/**").hasRole("ADMINISTRATOR")
                        // Customer order pages & HTMX fragments
                        .requestMatchers("/htmx/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/orders", "/api/orders/history").hasRole("CUSTOMER")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                // JWT Resource Server (for external API clients with Bearer token)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // OAuth2 Login (for browser-based Thymeleaf sessions)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService))
                        .defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(keycloakJwkSetUri)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    // Extract Keycloak roles from Access Token (more reliable than ID Token)
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(JwtDecoder jwtDecoder) {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            OAuth2AccessToken accessToken = userRequest.getAccessToken();

            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

            try {
                // Decode the Access Token to get realm_access
                Jwt jwt = jwtDecoder.decode(accessToken.getTokenValue());
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null && realmAccess.containsKey("roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) realmAccess.get("roles");
                    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                }

                // Also check resource_access for client roles
                Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
                if (resourceAccess != null) {
                    resourceAccess.forEach((client, access) -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> clientAccess = (Map<String, Object>) access;
                        if (clientAccess != null && clientAccess.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) clientAccess.get("roles");
                            roles.forEach(
                                    role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("Error decoding Access Token for roles: {}", e.getMessage());
            }

            // Merge with existing OIDC authorities (SCOPE_openid, etc.)
            var allAuthorities = Stream.concat(
                    oidcUser.getAuthorities().stream().map(a -> new SimpleGrantedAuthority(a.getAuthority())),
                    authorities.stream()).collect(Collectors.toSet());

            log.debug("Authenticated user: {}", oidcUser.getPreferredUsername());
            log.debug("Mapped authorities: {}", allAuthorities);

            return new DefaultOidcUser(allAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(securityProperties.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}