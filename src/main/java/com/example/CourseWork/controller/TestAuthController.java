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
    public String loginAsStaff(HttpServletRequest request) {
        return createMockAuth(request, "mock-staff-id", "test-staff-admin", "staff@smartdine.com", "ROLE_ADMINISTRATOR");
    }

    @GetMapping("/login-as-guest")
    public String loginAsGuest(HttpServletRequest request) {
        // Guests usually don't have roles, but we need a session context
        return createMockAuth(request, "mock-guest-id", "test-guest", "guest@smartdine.com");
    }

    @GetMapping("/login-as-customer")
    public String loginAsCustomer(HttpServletRequest request) {
        return createMockAuth(request, "mock-customer-id", "test-customer", "customer@smartdine.com", "ROLE_CUSTOMER");
    }

    private String createMockAuth(HttpServletRequest request, String id, String username, String email, String... roles) {
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
        
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        SecurityContextHolder.setContext(context);

        return "Successfully logged in as " + username + " with roles " + authorities;
    }
}
