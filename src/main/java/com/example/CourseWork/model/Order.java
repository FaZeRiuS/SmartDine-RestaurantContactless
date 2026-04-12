package com.example.CourseWork.model;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import jakarta.persistence.*;
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

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "total_price")
    private float totalPrice;

    @Column(name = "loyalty_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @Column(name = "loyalty_points_spent", nullable = false, precision = 19, scale = 2)
    private BigDecimal loyaltyPointsSpent = BigDecimal.ZERO;

    @Column(name = "tip_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "needs_waiter", nullable = false)
    private boolean needsWaiter = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();
}
