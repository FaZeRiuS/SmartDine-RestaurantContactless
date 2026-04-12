package com.example.CourseWork.dto;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDto {
    private Integer id;
    private String userId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private float totalPrice;
    private float loyaltyDiscount;
    private float amountToPay;
    private float tipAmount;
    private Integer serviceRating;
    private String serviceComment;
    private Integer tableNumber;
    private boolean needsWaiter;
    private List<OrderItemResponseDto> items;
}
