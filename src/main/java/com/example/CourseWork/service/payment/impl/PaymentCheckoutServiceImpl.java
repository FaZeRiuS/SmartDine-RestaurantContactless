package com.example.CourseWork.service.payment.impl;

import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.payment.LiqPayService;
import com.example.CourseWork.service.payment.PaymentCheckoutService;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import com.example.CourseWork.security.CurrentUserIdentity;
import org.springframework.stereotype.Service;

@Service
public class PaymentCheckoutServiceImpl implements PaymentCheckoutService {

    private final OrderRepository orderRepository;
    private final LiqPayService liqPayService;
    private final CurrentUserIdentity currentUserIdentity;
    private final OrderPaymentPolicy orderPaymentPolicy;

    public PaymentCheckoutServiceImpl(
            OrderRepository orderRepository,
            LiqPayService liqPayService,
            CurrentUserIdentity currentUserIdentity,
            OrderPaymentPolicy orderPaymentPolicy
    ) {
        this.orderRepository = orderRepository;
        this.liqPayService = liqPayService;
        this.currentUserIdentity = currentUserIdentity;
        this.orderPaymentPolicy = orderPaymentPolicy;
    }

    @Override
    public LiqPayCheckoutFormDto prepareCheckout(Integer orderId) {
        if (orderId == null) {
            throw new BadRequestException(ErrorMessages.ORDER_ID_REQUIRED);
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        String currentUserId = currentUserIdentity.currentUserId();
        orderPaymentPolicy.assertOwner(order, currentUserId);
        orderPaymentPolicy.assertNotPaid(order);

        return liqPayService.prepareCheckout(order);
    }
}

