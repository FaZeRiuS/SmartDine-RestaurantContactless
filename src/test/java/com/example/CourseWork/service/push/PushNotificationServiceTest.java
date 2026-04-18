package com.example.CourseWork.service.push;

import com.example.CourseWork.model.PushSubscription;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import com.example.CourseWork.service.push.impl.PushNotificationServiceImpl;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Security;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    static {
        // Essential for web-push library to parse P256 keys
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    @Mock
    private PushSubscriptionRepository subscriptionRepository;

    @Mock
    private PushService pushService;

    private PushNotificationServiceImpl pushNotificationService;

    private String validP256dh;
    private String validAuth;

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationServiceImpl(
                subscriptionRepository, "pub", "priv", "sub"
        );
        ReflectionTestUtils.setField(pushNotificationService, "pushService", pushService);

        // Generate a real valid P256 key pair using BouncyCastle
        try {
            java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
            java.security.KeyPair kp = kpg.generateKeyPair();
            byte[] pubKey = ((org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic()).getQ().getEncoded(false);
            validP256dh = Base64.getEncoder().encodeToString(pubKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test keys", e);
        }
        
        validAuth = Base64.getEncoder().encodeToString(new byte[16]);
    }

    @Test
    void sendNotificationToUser_ShouldSendToAllUserSubscriptions() throws Exception {
        // Arrange
        String userId = "user-1";
        PushSubscription sub1 = new PushSubscription();
        sub1.setEndpoint("https://updates.push.services.mozilla.com/ep1");
        sub1.setP256dh(validP256dh);
        sub1.setAuth(validAuth);
        
        PushSubscription sub2 = new PushSubscription();
        sub2.setEndpoint("https://fcm.googleapis.com/ep2");
        sub2.setP256dh(validP256dh);
        sub2.setAuth(validAuth);
        
        when(subscriptionRepository.findAllByUserId(userId)).thenReturn(List.of(sub1, sub2));

        // Act
        pushNotificationService.sendNotificationToUser(userId, "Hello");

        // Assert - adding timeout just in case @Async causes issues even with direct instantiation
        verify(pushService, timeout(1000).times(2)).send(any(Notification.class));
    }

    @Test
    void send_ShouldDeleteSubscriptionOn410Gone() throws Exception {
        // Arrange
        PushSubscription sub = new PushSubscription();
        sub.setEndpoint("https://ep-gone");
        sub.setP256dh(validP256dh);
        sub.setAuth(validAuth);
        when(subscriptionRepository.findAllByUserId("u1")).thenReturn(List.of(sub));
        
        // Simulate Gone (410) error
        doThrow(new RuntimeException("410 Gone")).when(pushService).send(any(Notification.class));

        // Act
        pushNotificationService.sendNotificationToUser("u1", "msg");

        // Assert
        verify(subscriptionRepository, timeout(1000)).delete(sub);
    }

    @Test
    void sendNotificationToRole_ShouldFetchByRole() throws Exception {
        // Arrange
        PushSubscription sub = new PushSubscription();
        sub.setEndpoint("https://admin-ep");
        sub.setP256dh(validP256dh);
        sub.setAuth(validAuth);
        when(subscriptionRepository.findAllByRolesContaining("ADMIN")).thenReturn(List.of(sub));

        // Act
        pushNotificationService.sendNotificationToRole("ADMIN", "Admin alert");

        // Assert
        verify(subscriptionRepository, timeout(1000)).findAllByRolesContaining("ADMIN");
        verify(pushService, timeout(1000)).send(any(Notification.class));
    }
}
