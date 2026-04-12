package com.example.CourseWork.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String endpoint;

    private String p256dh;
    private String auth;

    @Column(name = "user_id")
    private String userId;

    private String roles; // Store roles to filter notifications
}
