package com.example.CourseWork.service.payment.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.loyalty.LoyaltyService;
import com.example.CourseWork.service.payment.PaymentCallbackService;
import com.example.CourseWork.service.sse.SseService;
import com.example.CourseWork.util.LiqPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.CourseWork.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        Order order = orderRepository.findByIdForUpdate(dbOrderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            return; // idempotent: already processed
        }

        // Replay Attack protection: check if transaction has already been processed
        String paymentIdStr = callback.getPaymentId();
        if (paymentIdStr != null && !paymentIdStr.isBlank()) {
            if (orderRepository.existsByPaymentTransactionId(paymentIdStr)) {
                return; // already processed
            }
        }

        // Amount Validation: verify amount paid matches the order total
        BigDecimal total = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal discount = order.getLoyaltyDiscount() != null ? order.getLoyaltyDiscount() : BigDecimal.ZERO;
        BigDecimal tip = order.getTipAmount() != null ? order.getTipAmount() : BigDecimal.ZERO;
        BigDecimal expectedPayable = total.subtract(discount).add(tip);
        if (expectedPayable.compareTo(BigDecimal.ZERO) < 0) {
            expectedPayable = BigDecimal.ZERO;
        }
        expectedPayable = expectedPayable.setScale(2, RoundingMode.HALF_UP);

        if (callback.getAmount() == null) {
            throw new BadRequestException("Missing payment amount in callback");
        }
        BigDecimal callbackAmount;
        try {
            callbackAmount = new BigDecimal(callback.getAmount()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new BadRequestException("Invalid payment amount format: " + callback.getAmount());
        }

        // Check if amount matches expected total (allow up to 0.02 UAH epsilon difference due to rounding)
        if (callbackAmount.subtract(expectedPayable).abs().compareTo(new BigDecimal("0.02")) > 0) {
            throw new BadRequestException("Payment amount mismatch: paid " + callbackAmount + ", expected " + expectedPayable);
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        if (OrderStatus.READY.equals(order.getStatus())) {
            order.setStatus(OrderStatus.COMPLETED);
        }
        if (paymentIdStr != null && !paymentIdStr.isBlank()) {
            order.setPaymentTransactionId(paymentIdStr);
        }
        orderRepository.save(order);

        earnCashbackIfEligible(order);
    }

    @Override
    @Transactional
    public void handleCallbackFailure(LiqPayCallbackDto callback) {
        Integer dbOrderId = LiqPayUtil.extractDbOrderId(callback.getOrderId());
        @SuppressWarnings("null")
        Order order = orderRepository.findByIdForUpdate(dbOrderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            return; // idempotent: do not overwrite a successful payment status
        }

        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
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
