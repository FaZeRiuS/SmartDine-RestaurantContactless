package com.example.CourseWork.controller;

import com.example.CourseWork.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping("/api/sse/subscribe/{userId}")
    public SseEmitter subscribe(@PathVariable String userId, Authentication auth) {
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR") || 
                               a.getAuthority().equals("ROLE_CHEF") || 
                               a.getAuthority().equals("ROLE_WAITER"));
        
        return sseService.subscribe(userId, isStaff);
    }
}
