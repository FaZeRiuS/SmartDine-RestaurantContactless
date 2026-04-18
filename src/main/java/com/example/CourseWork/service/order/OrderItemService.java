package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;

public interface OrderItemService {
    OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto);
    OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity);
    OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest);
    OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId);
    OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber);
}

