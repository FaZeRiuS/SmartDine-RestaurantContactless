package com.example.CourseWork.service.security;

import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class CurrentUserIdentity {

    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            // JWT authentication (API clients with Bearer token)
            if (principal instanceof Jwt jwt) {
                return jwt.getSubject();
            }

            // OIDC authentication (browser sessions via OAuth2 Login)
            if (principal instanceof OidcUser oidcUser) {
                return oidcUser.getSubject();
            }
        }

        // Not authenticated: return a guest identity based on Session ID
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String sessionId = attr.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                throw new UnauthorizedException(ErrorMessages.COULD_NOT_IDENTIFY_USER_OR_GUEST_SESSION);
            }
            return "GUEST_" + sessionId;
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorMessages.COULD_NOT_IDENTIFY_USER_OR_GUEST_SESSION);
        }
    }

    public boolean isGuest() {
        String id = currentUserId();
        return id != null && id.startsWith("GUEST_");
    }

    public UUID requireCustomerUuid(String message) {
        String raw = currentUserId();
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            throw new UnauthorizedException(message != null ? message : "Customer account is required");
        }
    }

    public UUID requireCustomerUuid() {
        return requireCustomerUuid("Customer account is required");
    }
}

