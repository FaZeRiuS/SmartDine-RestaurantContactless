package com.example.CourseWork.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Cache key for {@link com.example.CourseWork.service.impl.MenuServiceImpl#getAllMenusWithDishes()}.
 * Includes a 15-minute time bucket so menu availability windows can refresh without long-lived stale HTML.
 */
@Component("publicPageCacheKey")
public class PublicPageCacheKey {

    public String menusWithDishesKey() {
        LocalTime now = LocalTime.now();
        int slot = now.getHour() * 4 + now.getMinute() / 15;
        return menusRoleKey() + ":" + slot;
    }

    private static String menusRoleKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "guest";
        }
        boolean staff = auth.getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_WAITER".equals(r) || "ROLE_CHEF".equals(r) || "ROLE_ADMINISTRATOR".equals(r);
        });
        if (staff) {
            return "staff";
        }
        if (auth.getPrincipal() instanceof OidcUser oidcUser) {
            return "user:" + oidcUser.getSubject();
        }
        return "user:" + auth.getName();
    }
}
