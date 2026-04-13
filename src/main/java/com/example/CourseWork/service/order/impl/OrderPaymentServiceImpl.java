package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderPaymentService;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotifier orderNotifier;
    private final OrderPaymentPolicy orderPaymentPolicy;

    public OrderPaymentServiceImpl(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            OrderNotifier orderNotifier,
            OrderPaymentPolicy orderPaymentPolicy
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderNotifier = orderNotifier;
        this.orderPaymentPolicy = orderPaymentPolicy;
    }

    @Transactional
    @Override
    public OrderResponseDto payOrder(Integer id, String userId, String paymentMethodId) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderPaymentPolicy.assertOwner(order, userId);
        orderPaymentPolicy.assertNotPaid(order);

        // Unified flow: payment is initiated via LiqPay checkout form (/api/payment/init)
        // and finalized via signed callback (/api/payment/callback).
        // This method keeps API compatibility and ensures the order is in PENDING state.
        order.setPaymentStatus(PaymentStatus.PENDING);

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(savedOrder);
        orderNotifier.notifyUserOfUpdate(order.getUserId(), response);
        return response;
    }
}

