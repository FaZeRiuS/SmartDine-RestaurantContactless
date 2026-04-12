package com.example.CourseWork.model;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PositiveOrZero
    @Column(name = "total_price", nullable = false)
    private float totalPrice;

    @NotNull
    @PositiveOrZero
    @Column(name = "loyalty_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "loyalty_points_spent", nullable = false, precision = 19, scale = 2)
    private BigDecimal loyaltyPointsSpent = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "tip_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Positive
    @Column(name = "table_number")
    private Integer tableNumber;

    @NotNull
    @Column(name = "needs_waiter", nullable = false)
    private boolean needsWaiter = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();
}
