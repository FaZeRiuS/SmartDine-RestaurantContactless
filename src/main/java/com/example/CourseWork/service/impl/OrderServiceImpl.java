package com.example.CourseWork.service.impl;

import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.service.order.OrderCreateService;
import com.example.CourseWork.service.order.OrderItemService;
import com.example.CourseWork.service.order.OrderPaymentService;
import com.example.CourseWork.service.order.OrderReadService;
import com.example.CourseWork.service.order.OrderWaiterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderCreateService orderCreateService;
    private final OrderReadService orderReadService;
    private final OrderItemService orderItemService;
    private final OrderPaymentService orderPaymentService;
    private final OrderWaiterService orderWaiterService;

    @Override
    public OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber) {
        return orderCreateService.createOrder(userId, dto, tableNumber);
    }

    @Transactional
    @Override
    public Page<OrderResponseDto> getAllOrders(Pageable pageable) {
        return orderReadService.getAllOrders(pageable);
    }

    @Transactional
    @Override
    public OrderResponseDto getOrderById(Integer id) {
        return orderReadService.getOrderById(id);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getNewOrders() {
        return orderReadService.getNewOrders();
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity) {
        return orderItemService.updateOrderItemQuantity(orderId, userId, itemId, quantity);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest) {
        return orderItemService.updateOrderItemSpecialRequest(orderId, userId, itemId, specialRequest);
    }

    @Transactional
    @Override
    public OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId) {
        return orderItemService.removeOrderItem(orderId, userId, itemId);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getActiveOrders() {
        return orderReadService.getActiveOrders();
    }

    @Transactional
    public OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber) {
        return orderItemService.confirmOrderFromCart(userId, tableNumber);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderStatus(Integer Id, OrderStatus newStatus) {
        return orderReadService.updateOrderStatus(Id, newStatus);
    }

    @Transactional
    @Override
    public OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto) {
        return orderItemService.addItemsToOrder(orderId, userId, dto);
    }

    @Transactional
    @Override
    public OrderResponseDto payOrder(Integer id, String userId, String paymentMethodId) {
        return orderPaymentService.payOrder(id, userId, paymentMethodId);
    }

    @Transactional
    @Override
    public Optional<OrderResponseDto> getMyActiveOrder(String userId) {
        return orderReadService.getMyActiveOrder(userId);
    }

    @Transactional
    @Override
    public OrderResponseDto callWaiter(Integer orderId) {
        return orderWaiterService.callWaiter(orderId);
    }

    @Transactional
    @Override
    public OrderResponseDto dismissWaiterCall(Integer orderId) {
        return orderWaiterService.dismissWaiterCall(orderId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponseDto> getOrderHistory(String userId, Pageable pageable) {
        return orderReadService.getOrderHistory(userId, pageable);
    }
}
