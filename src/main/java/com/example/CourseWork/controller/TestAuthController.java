package com.example.CourseWork.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Backdoor authentication controller for E2E testing.
 * ONLY ENABLED in 'test' profile.
 */
@RestController
@RequestMapping("/api/test/auth")
@Profile("test")
public class TestAuthController {

    @GetMapping("/login-as-staff")
    public String loginAsStaff(HttpServletRequest request, HttpServletResponse response) {
        return createMockAuth(request, response, "00000000-0000-0000-0000-000000000000", "test-staff-admin", "staff@smartdine.com", "ROLE_ADMINISTRATOR");
    }

    @GetMapping("/login-as-guest")
    public String loginAsGuest(HttpServletRequest request, HttpServletResponse response) {
        // Use a fresh identity per E2E run to avoid leaking cart/orders state across tests in the same JVM.
        String id = java.util.UUID.randomUUID().toString();
        String suffix = id.substring(0, 8);
        return createMockAuth(request, response, id, "test-guest-" + suffix, "guest-" + suffix + "@smartdine.com");
    }

    @GetMapping("/login-as-customer")
    public String loginAsCustomer(HttpServletRequest request, HttpServletResponse response) {
        return createMockAuth(request, response, "00000000-0000-0000-0000-000000000002", "test-customer", "customer@smartdine.com", "ROLE_CUSTOMER");
    }

    private String createMockAuth(HttpServletRequest request, HttpServletResponse response, String id, String username, String email, String... roles) {
        Map<String, Object> claims = Map.of(
            "sub", id,
            "preferred_username", username,
            "email", email
        );
        OidcIdToken idToken = new OidcIdToken("fake-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken);
        Authentication auth = new UsernamePasswordAuthenticationToken(oidcUser, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        
        SecurityContextHolder.setContext(context);

        // Spring Security 6+: persist the context explicitly so the session is authenticated on subsequent requests.
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        return "Successfully logged in as " + username + " with roles " + authorities;
    }
}
