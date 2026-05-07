package com.example.CourseWork.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GuestSessionFilter extends OncePerRequestFilter {

    public static final String PREVIOUS_GUEST_ID = "PREVIOUS_GUEST_ID";

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request, 
            @org.springframework.lang.NonNull HttpServletResponse response, 
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // If the user is unauthenticated or an anonymous user, we save their guest ID
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            HttpSession session = request.getSession(true);
            session.setAttribute(PREVIOUS_GUEST_ID, "GUEST_" + session.getId());
        }

        filterChain.doFilter(request, response);
    }
}
