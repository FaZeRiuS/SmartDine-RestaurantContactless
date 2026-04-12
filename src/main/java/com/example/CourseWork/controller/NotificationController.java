package com.example.CourseWork.controller;

import com.example.CourseWork.dto.PushSubscriptionDto;
import com.example.CourseWork.model.PushSubscription;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import com.example.CourseWork.util.KeycloakUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushSubscriptionRepository subscriptionRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionDto dto) {
        String userId = KeycloakUtil.getCurrentUser().getId();
        
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
    public ResponseEntity<Void> unsubscribe(@RequestBody PushSubscriptionDto dto) {
        subscriptionRepository.findByEndpoint(dto.getEndpoint())
                .ifPresent(subscriptionRepository::delete);
        return ResponseEntity.ok().build();
    }
}
