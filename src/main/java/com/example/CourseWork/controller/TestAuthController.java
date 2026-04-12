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
import java.util.Collections;
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
        // 1. Create Mock OIDC User with ADMINISTRATOR role
        Map<String, Object> claims = Map.of(
            "sub", "mock-staff-id",
            "preferred_username", "test-staff-admin",
            "email", "staff@smartdine.com"
        );
        OidcIdToken idToken = new OidcIdToken("fake-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken);

        // 2. Create Authentication object
        Authentication auth = new UsernamePasswordAuthenticationToken(oidcUser, null, authorities);

        // 3. Set into SecurityContext
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // 4. Persist in Session so Playwright can maintain state
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "Successfully logged in as STAFF (ADMINISTRATOR)";
    }
}
