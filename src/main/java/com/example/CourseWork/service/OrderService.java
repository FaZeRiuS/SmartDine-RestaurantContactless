package com.example.CourseWork.service;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber);
    List<OrderResponseDto> getAllOrders();
    OrderResponseDto getOrderById(Integer id);
    List<OrderResponseDto> getNewOrders();
    List<OrderResponseDto> getActiveOrders();
    OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber);
    OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto);
    OrderResponseDto updateOrderStatus(Integer orderId, OrderStatus newStatus);
    OrderResponseDto payOrder(Integer orderId, String userId, String paymentMethodId);
    OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity);
    OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest);
    OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId);
    OrderResponseDto getMyActiveOrder(String userId);
    List<OrderResponseDto> getOrderHistory(String userId);
    OrderResponseDto callWaiter(Integer orderId);
    OrderResponseDto dismissWaiterCall(Integer orderId);
}
