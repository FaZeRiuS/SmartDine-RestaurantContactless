package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.service.order.OrderCreateService;
import com.example.CourseWork.service.order.OrderItemService;
import com.example.CourseWork.service.order.OrderPaymentService;
import com.example.CourseWork.service.order.OrderReadService;
import com.example.CourseWork.service.order.OrderWaiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponseDto> getAllOrdersFiltered(Pageable pageable, OrderStatus status, PaymentStatus paymentStatus) {
        return orderReadService.getAllOrdersFiltered(pageable, status, paymentStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDto getOrderDetailForAdmin(Integer id) {
        return orderReadService.getOrderDetailForAdmin(id);
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
    public List<OrderResponseDto> getActiveOrders() {
        return orderReadService.getActiveOrders();
    }

    @Transactional
    @Override
    public OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber) {
        return orderItemService.confirmOrderFromCart(userId, tableNumber);
    }

    @Transactional
    @Override
    public OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto) {
        return orderItemService.addItemsToOrder(orderId, userId, dto);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        return orderReadService.updateOrderStatus(orderId, newStatus);
    }

    @Transactional
    @Override
    public OrderResponseDto payOrder(Integer orderId, String userId, String paymentMethodId) {
        return orderPaymentService.payOrder(orderId, userId, paymentMethodId);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity) {
        return orderItemService.updateOrderItemQuantity(orderId, userId, itemId, quantity);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemSpecialRequest(
            Integer orderId,
            String userId,
            Integer itemId,
            String specialRequest
    ) {
        return orderItemService.updateOrderItemSpecialRequest(orderId, userId, itemId, specialRequest);
    }

    @Transactional
    @Override
    public OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId) {
        return orderItemService.removeOrderItem(orderId, userId, itemId);
    }

    @Transactional
    @Override
    public Optional<OrderResponseDto> getMyActiveOrder(String userId) {
        return orderReadService.getMyActiveOrder(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponseDto> getOrderHistory(String userId, Pageable pageable) {
        return orderReadService.getOrderHistory(userId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponseDto> getOrderHistoryForStatuses(
            String userId,
            Collection<OrderStatus> statuses,
            Pageable pageable
    ) {
        return orderReadService.getOrderHistoryForStatuses(userId, statuses, pageable);
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
}
