package com.example.CourseWork.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "Endpoint is required")
    @Size(max = 2048)
    @Column(nullable = false, unique = true, length = 2048)
    @ToString.Include
    private String endpoint;

    @Size(max = 255)
    @ToString.Include
    private String p256dh;

    @Size(max = 255)
    @ToString.Include
    private String auth;

    @Column(name = "user_id")
    @ToString.Include
    private String userId;

    @Size(max = 255)
    @ToString.Include
    private String roles; // Store roles to filter notifications
}
