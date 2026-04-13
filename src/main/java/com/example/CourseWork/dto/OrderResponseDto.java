package com.example.CourseWork.dto;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OrderResponseDto {
    private Integer id;
    private String userId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private OffsetDateTime createdAt;
    private BigDecimal totalPrice;
    private BigDecimal loyaltyDiscount;
    private BigDecimal amountToPay;
    private BigDecimal tipAmount;
    private Integer serviceRating;
    private String serviceComment;
    private Integer tableNumber;
    private boolean needsWaiter;
    private List<OrderItemResponseDto> items;
}
