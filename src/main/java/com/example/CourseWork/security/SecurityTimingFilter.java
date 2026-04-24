package com.example.CourseWork.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to measure the time taken by the Security Filter Chain.
 * This helps verify the requirement that authorization time is under 200ms.
 */
@Component
public class SecurityTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityTimingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Only log for API requests or as needed
            if (request.getRequestURI().startsWith("/api/")) {
                if (duration > 200) {
                    log.warn("SECURITY ALERT: Authorization for {} took {} ms (exceeds 200ms limit!)", 
                            request.getRequestURI(), duration);
                } else {
                    log.debug("Security authorization for {} took {} ms", request.getRequestURI(), duration);
                }
                
                // Add header to response for visibility in browser/tests
                response.addHeader("X-Security-Time-Ms", String.valueOf(duration));
            }
        }
    }
}
