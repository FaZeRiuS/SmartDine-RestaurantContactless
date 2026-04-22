package com.example.CourseWork.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "order_service_review",
        uniqueConstraints = @UniqueConstraint(name = "ux_order_service_review_order_id", columnNames = {"order_id"})
)
public class OrderServiceReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private String userId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    @ToString.Include
    private Integer rating;

    @Size(max = 2000)
    @Column(name = "comment", columnDefinition = "TEXT")
    @ToString.Include
    private String comment;

    @NotNull
    @Column(name = "created_at", nullable = false)
    @ToString.Include
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    @ToString.Include
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

