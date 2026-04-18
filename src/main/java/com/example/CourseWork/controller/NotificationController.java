package com.example.CourseWork.controller;

import com.example.CourseWork.dto.push.PushSubscriptionDto;
import com.example.CourseWork.model.PushSubscription;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import com.example.CourseWork.security.CurrentUserIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final PushSubscriptionRepository subscriptionRepository;
    private final CurrentUserIdentity currentUserIdentity;

    /**
     * Matches {@code ui.js} {@code shouldSkipServerLogForPwa}: expected on many devices (no Google Play
     * services, blocked push endpoints, missing VAPID env in dev).
     */
    private static boolean isExpectedPushClientNoise(String message) {
        if (message == null) {
            return false;
        }
        return message.startsWith("PWA: Failed to subscribe user:")
                || message.startsWith("PWA: VAPID public key not found");
    }

    @PostMapping("/log")
    public ResponseEntity<Void> logClientError(@RequestBody String message) {
        String userId = currentUserIdentity.currentUserId();
        if (isExpectedPushClientNoise(message)) {
            if (log.isDebugEnabled()) {
                log.debug("[PWA] expected client limitation user={} message={}", userId, message);
            }
            return ResponseEntity.ok().build();
        }
        log.error("[PWA-ERROR] user: {}, message: {}", userId, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionDto dto) {
        String userId = currentUserIdentity.currentUserId();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(","));

        PushSubscription sub = subscriptionRepository.findByEndpoint(dto.getEndpoint())
                .orElse(new PushSubscription());
        
        sub.setEndpoint(dto.getEndpoint());
        sub.setP256dh(dto.getP256dh());
        sub.setAuth(dto.getAuth());
        sub.setUserId(userId);
        sub.setRoles(roles);

        subscriptionRepository.save(sub);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushSubscriptionDto dto) {
        subscriptionRepository.findByEndpoint(dto.getEndpoint())
                .ifPresent(subscriptionRepository::delete);
        return ResponseEntity.ok().build();
    }
}
