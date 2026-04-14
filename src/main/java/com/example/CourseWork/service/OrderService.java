package com.example.CourseWork.service;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber);

    Page<OrderResponseDto> getAllOrders(Pageable pageable);

    Page<OrderResponseDto> getAllOrdersFiltered(Pageable pageable, OrderStatus status, PaymentStatus paymentStatus);

    OrderResponseDto getOrderById(Integer id);

    OrderResponseDto getOrderDetailForAdmin(Integer id);

    List<OrderResponseDto> getNewOrders();

    List<OrderResponseDto> getActiveOrders();

    OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber);

    OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto);

    OrderResponseDto updateOrderStatus(Integer orderId, OrderStatus newStatus);

    OrderResponseDto payOrder(Integer orderId, String userId, String paymentMethodId);

    OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity);

    OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest);

    OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId);

    Optional<OrderResponseDto> getMyActiveOrder(String userId);

    Page<OrderResponseDto> getOrderHistory(String userId, Pageable pageable);

    Page<OrderResponseDto> getOrderHistoryForStatuses(String userId, Collection<OrderStatus> statuses, Pageable pageable);

    OrderResponseDto callWaiter(Integer orderId);

    OrderResponseDto dismissWaiterCall(Integer orderId);
}
