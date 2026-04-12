package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.OrderTipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderTipServiceImpl implements OrderTipService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal TIP_MAX = new BigDecimal("10000.00");

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDto setTip(Integer orderId, UUID userId, BigDecimal amount) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        if (userId == null) throw new IllegalArgumentException("userId is required");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getUserId() == null || !order.getUserId().equals(userId.toString())) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }
        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order is already paid");
        }

        BigDecimal tip = normalizeMoney(amount);
        if (tip.compareTo(BigDecimal.ZERO) < 0) tip = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (tip.compareTo(TIP_MAX) > 0) tip = TIP_MAX;

        order.setTipAmount(tip);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponseDto(saved);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

