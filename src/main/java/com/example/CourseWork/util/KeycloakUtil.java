package com.example.CourseWork.util;

import com.example.CourseWork.model.KeycloakUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class KeycloakUtil {
    
    public static KeycloakUser getCurrentUser() {
        KeycloakUser user = getCurrentUserOrNull();
        if (user != null) {
            return user;
        }
        
        // If not authenticated, return a Guest user based on Session ID
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String sessionId = attr.getSessionId();
            KeycloakUser guest = new KeycloakUser();
            guest.setId("GUEST_" + sessionId);
            guest.setUsername("Гість");
            return guest;
        } catch (Exception e) {
            throw new RuntimeException("Could not identify user or guest session");
        }
    }

    public static KeycloakUser getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        // JWT authentication (API clients with Bearer token)
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            KeycloakUser user = new KeycloakUser();
            user.setId(jwt.getSubject());
            user.setUsername(jwt.getClaimAsString("preferred_username"));
            user.setEmail(jwt.getClaimAsString("email"));
            user.setFirstName(jwt.getClaimAsString("given_name"));
            user.setLastName(jwt.getClaimAsString("family_name"));
            return user;
        }

        // OIDC authentication (browser sessions via OAuth2 Login)
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            KeycloakUser user = new KeycloakUser();
            user.setId(oidcUser.getSubject());
            user.setUsername(oidcUser.getPreferredUsername());
            user.setEmail(oidcUser.getEmail());
            user.setFirstName(oidcUser.getGivenName());
            user.setLastName(oidcUser.getFamilyName());
            return user;
        }

        return null;
    }
}