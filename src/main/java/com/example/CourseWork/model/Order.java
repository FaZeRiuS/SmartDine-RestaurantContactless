package com.example.CourseWork.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private OrderStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    @ToString.Include
    private PaymentStatus paymentStatus;

    @NotNull
    @Column(name = "created_at", nullable = false)
    @ToString.Include
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "estimated_ready_time")
    @ToString.Include
    private OffsetDateTime estimatedReadyTime;

    @PositiveOrZero
    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "loyalty_discount", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "loyalty_points_spent", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal loyaltyPointsSpent = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "tip_amount", nullable = false, precision = 19, scale = 2)
    @ToString.Include
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "tip_opt_out", nullable = false)
    @ToString.Include
    private boolean tipOptOut = false;

    @Positive
    @Column(name = "table_number")
    @ToString.Include
    private Integer tableNumber;

    @NotNull
    @Column(name = "needs_waiter", nullable = false)
    @ToString.Include
    private boolean needsWaiter = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();
}
