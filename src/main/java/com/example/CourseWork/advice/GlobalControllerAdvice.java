package com.example.CourseWork.advice;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@lombok.RequiredArgsConstructor
@Slf4j
public class GlobalControllerAdvice {

    private final com.example.CourseWork.service.LoyaltyService loyaltyService;
    private final com.example.CourseWork.service.security.CurrentUserIdentity currentUserIdentity;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {
        // Add current URI for navigation highlighting
        model.addAttribute("currentUri", request.getRequestURI());

        // Add VAPID key to all models for PWA
        model.addAttribute("vapidPublicKey", vapidPublicKey);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OidcUser oidcUser) {
            String username = oidcUser.getPreferredUsername();
            if (username == null || username.isEmpty()) {
                username = oidcUser.getName(); // Fallback to 'sub' or user ID
            }
            model.addAttribute("username", username);

            // Fetch loyalty info if CUSTOMER
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                try {
                    java.util.UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for loyalty");
                    model.addAttribute("loyaltyBalance", loyaltyService.getBalance(userId));
                    model.addAttribute("cashbackRate", loyaltyService.resolveCashbackRate(userId));
                } catch (Exception e) {
                    log.error("Error fetching loyalty balance for user {}: {}", oidcUser.getSubject(), e.getMessage());
                }
            }
        }
    }
}
