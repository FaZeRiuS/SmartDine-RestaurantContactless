package com.example.CourseWork.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "loyalty_account", uniqueConstraints = {
        @UniqueConstraint(name = "ux_loyalty_account_user_id", columnNames = { "user_id" })
})
public class LoyaltyAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    @ToString.Include
    private java.util.UUID userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    @ToString.Include
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @ToString.Include
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
        if (balance == null)
            balance = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
        if (balance == null)
            balance = BigDecimal.ZERO;
    }
}
