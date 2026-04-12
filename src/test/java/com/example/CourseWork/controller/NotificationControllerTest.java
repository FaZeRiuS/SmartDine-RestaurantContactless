package com.example.CourseWork.controller;

import com.example.CourseWork.dto.PushSubscriptionDto;
import com.example.CourseWork.model.PushSubscription;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@SuppressWarnings("null")
class NotificationControllerTest extends BaseControllerTest {

    @MockitoBean
    private PushSubscriptionRepository subscriptionRepository;

    @Test
    void subscribe_ShouldSaveSubscriptionWithUserAndRoles() throws Exception {
        // Arrange
        PushSubscriptionDto dto = new PushSubscriptionDto();
        dto.setEndpoint("https://fcm.googleapis.com/test");
        dto.setP256dh("p256");
        dto.setAuth("auth-secret");

        when(subscriptionRepository.findByEndpoint(any())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/notifications/subscribe")
                        .with(withUser("user-123", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://fcm.googleapis.com/test\",\"p256dh\":\"p256\",\"auth\":\"auth-secret\"}"))
                .andExpect(status().isOk());

        verify(subscriptionRepository).save(argThat(sub -> 
            sub.getUserId().equals("user-123") && 
            sub.getRoles().contains("ROLE_CUSTOMER") &&
            sub.getEndpoint().equals("https://fcm.googleapis.com/test")
        ));
    }

    @Test
    void unsubscribe_ShouldRemoveSubscription() throws Exception {
        // Arrange
        PushSubscription existing = new PushSubscription();
        existing.setEndpoint("https://fcm.googleapis.com/test");
        when(subscriptionRepository.findByEndpoint("https://fcm.googleapis.com/test"))
                .thenReturn(Optional.of(existing));

        // Act & Assert
        mockMvc.perform(post("/api/notifications/unsubscribe")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(withUser("user-123", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://fcm.googleapis.com/test\"}"))
                .andExpect(status().isOk());

        verify(subscriptionRepository).delete(existing);
    }
}
