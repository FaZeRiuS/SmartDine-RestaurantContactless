package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderWaiterService;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.i18n.NotificationMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWaiterServiceImpl implements OrderWaiterService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotifier orderNotifier;

    public OrderWaiterServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper, OrderNotifier orderNotifier) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderNotifier = orderNotifier;
    }

    @Transactional
    @Override
    public OrderResponseDto callWaiter(Integer orderId) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(ErrorMessages.CANNOT_CALL_WAITER_FOR_COMPLETED_OR_CANCELLED);
        }

        order.setNeedsWaiter(true);
        orderRepository.save(order);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        orderNotifier.sendStaffNotification(NotificationMessages.staffWaiterCalled(order.getTableNumber()));
        orderNotifier.sendPushToRole(NotificationMessages.ROLE_WAITER,
                NotificationMessages.pushWaiterNeeded(order.getTableNumber()));

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto dismissWaiterCall(Integer orderId) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        order.setNeedsWaiter(false);
        orderRepository.save(order);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        orderNotifier.notifyUserOfUpdate(order.getUserId(), response);

        return response;
    }
}

