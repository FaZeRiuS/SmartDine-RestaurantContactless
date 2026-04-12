package com.example.CourseWork.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Endpoint is required")
    @Size(max = 2048)
    @Column(nullable = false, unique = true, length = 2048)
    private String endpoint;

    @Size(max = 255)
    private String p256dh;

    @Size(max = 255)
    private String auth;

    @Column(name = "user_id")
    private String userId;

    @Size(max = 255)
    private String roles; // Store roles to filter notifications
}
