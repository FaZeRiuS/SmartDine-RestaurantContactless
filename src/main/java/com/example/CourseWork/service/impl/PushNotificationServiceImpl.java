package com.example.CourseWork.service.impl;

import com.example.CourseWork.model.PushSubscription;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import com.example.CourseWork.service.PushNotificationService;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    private PushService pushService;

    public PushNotificationServiceImpl(
            PushSubscriptionRepository subscriptionRepository,
            @Value("${vapid.public.key}") String publicKey,
            @Value("${vapid.private.key}") String privateKey,
            @Value("${vapid.subject}") String subject) {
        this.subscriptionRepository = subscriptionRepository;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    public void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            this.pushService = new PushService(publicKey, privateKey, subject);
            System.out.println(">>> PWA: PushNotificationService initialized successfully");
        } catch (Exception e) {
            System.err.println(">>> PWA ERROR: Failed to initialize PushNotificationService: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendNotificationToUser(String userId, String payload) {
        List<PushSubscription> subs = subscriptionRepository.findAllByUserId(userId);
        for (PushSubscription sub : subs) {
            send(sub, payload);
        }
    }

    @Override
    @Async
    public void sendNotificationToRole(String role, String payload) {
        List<PushSubscription> subs = subscriptionRepository.findAllByRolesContaining(role);
        for (PushSubscription sub : subs) {
            send(sub, payload);
        }
    }

    private void send(PushSubscription sub, String payload) {
        try {
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload
            );
            pushService.send(notification);
        } catch (Exception e) {
            System.err.println("Failed to send push notification to " + sub.getEndpoint() + ": " + e.getMessage());
            if (e.getMessage().contains("410") || e.getMessage().contains("404")) {
                subscriptionRepository.delete(sub);
            }
        }
    }
}
