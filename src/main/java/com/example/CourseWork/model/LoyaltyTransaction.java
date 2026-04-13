package com.example.CourseWork.model;

import com.example.CourseWork.addition.LoyaltyTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
@Table(name = "loyalty_transaction")
public class LoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    @ToString.Include
    private LoyaltyTransactionType type;

    @NotNull
    @PositiveOrZero
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal amount;

    @NotNull
    @PositiveOrZero
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal balanceAfter;

    @Size(max = 255)
    @Column(name = "reference", length = 255)
    @ToString.Include
    private String reference;

    @NotNull
    @Column(name = "created_at", nullable = false)
    @ToString.Include
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

