package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.LiqPayCallbackDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.LoyaltyService;
import com.example.CourseWork.service.PaymentCallbackService;
import com.example.CourseWork.service.SseService;
import com.example.CourseWork.util.LiqPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private final OrderRepository orderRepository;
    private final LoyaltyService loyaltyService;
    private final OrderMapper orderMapper;
    private final SseService sseService;

    public PaymentCallbackServiceImpl(
            OrderRepository orderRepository,
            LoyaltyService loyaltyService,
            OrderMapper orderMapper,
            SseService sseService
    ) {
        this.orderRepository = orderRepository;
        this.loyaltyService = loyaltyService;
        this.orderMapper = orderMapper;
        this.sseService = sseService;
    }

    @Override
    @Transactional
    public void handleCallbackSuccess(LiqPayCallbackDto callback) {
        Integer dbOrderId = LiqPayUtil.extractDbOrderId(callback.getOrderId());
        @SuppressWarnings("null")
        Order order = orderRepository.findById(dbOrderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            return; // idempotent: already processed
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        if (OrderStatus.READY.equals(order.getStatus())) {
            order.setStatus(OrderStatus.COMPLETED);
        }
        orderRepository.save(order);

        earnCashbackIfEligible(order);
    }

    @Override
    @Transactional(readOnly = true)
    public void publishOrderUpdateToUser(Integer dbOrderId) {
        @SuppressWarnings("null")
        Order order = orderRepository.findByIdWithItemsAndDishes(dbOrderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));
        OrderResponseDto response = orderMapper.toResponseDto(order);
        if (order.getUserId() != null) {
            sseService.sendOrderUpdate(order.getUserId(), response);
        }
    }

    private void earnCashbackIfEligible(Order order) {
        if (order == null || order.getUserId() == null || order.getUserId().isBlank()) {
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(order.getUserId());
        } catch (Exception e) {
            // Guest sessions and any non-UUID values are not eligible.
            return;
        }

        String reference = "LIQPAY:order:" + order.getId();
        BigDecimal total = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
        BigDecimal discount = order.getLoyaltyDiscount() != null ? order.getLoyaltyDiscount() : BigDecimal.ZERO;
        BigDecimal orderAmount = total.subtract(discount); // tips are excluded from cashback
        if (orderAmount.compareTo(BigDecimal.ZERO) < 0) {
            orderAmount = BigDecimal.ZERO;
        }
        loyaltyService.earnPointsInternal(userId, orderAmount, reference);
    }
}
