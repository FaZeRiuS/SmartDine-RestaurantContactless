package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderLoyaltyService;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
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
    private final com.example.CourseWork.service.loyalty.LoyaltyService loyaltyService;
    private final OrderMapper orderMapper;
    private final OrderPaymentPolicy orderPaymentPolicy;

    @Override
    @Transactional
    public OrderResponseDto applyCoverage(Integer orderId, UUID userId, BigDecimal desiredAmount) {
        if (orderId == null) throw new BadRequestException(ErrorMessages.ORDER_ID_REQUIRED);
        if (userId == null) throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderPaymentPolicy.assertOwner(order, userId.toString());
        orderPaymentPolicy.assertNotPaid(order);

        BigDecimal total = normalizeMoney(order.getTotalPrice());
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

