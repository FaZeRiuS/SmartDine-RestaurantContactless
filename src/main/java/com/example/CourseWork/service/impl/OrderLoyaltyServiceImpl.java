package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.OrderLoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderLoyaltyServiceImpl implements OrderLoyaltyService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal MAX_COVERAGE_RATE = new BigDecimal("0.50");

    private final OrderRepository orderRepository;
    private final com.example.CourseWork.service.LoyaltyService loyaltyService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDto applyCoverage(Integer orderId, UUID userId, BigDecimal desiredAmount) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        if (userId == null) throw new IllegalArgumentException("userId is required");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getUserId() == null || !order.getUserId().equals(userId.toString())) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus().name().equalsIgnoreCase("SUCCESS")) {
            throw new RuntimeException("Order is already paid");
        }

        BigDecimal total = BigDecimal.valueOf(order.getTotalPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal maxCover = total.multiply(MAX_COVERAGE_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal desired = normalizeMoney(desiredAmount);
        if (desired.compareTo(BigDecimal.ZERO) < 0) desired = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (desired.compareTo(maxCover) > 0) desired = maxCover;

        BigDecimal current = normalizeMoney(order.getLoyaltyDiscount());
        if (current.compareTo(BigDecimal.ZERO) < 0) current = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (current.compareTo(total) > 0) current = total;

        BigDecimal delta = desired.subtract(current).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            String reference = "ORDER:" + orderId + ":SPEND_TO:" + desired;
            loyaltyService.spendPointsInternal(userId, delta, reference);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            String reference = "ORDER:" + orderId + ":REFUND_TO:" + desired;
            // refund points by crediting the exact difference back
            loyaltyService.creditPointsInternal(userId, delta.abs(), reference);
        }

        order.setLoyaltyDiscount(desired);
        order.setLoyaltyPointsSpent(desired);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponseDto(saved);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

