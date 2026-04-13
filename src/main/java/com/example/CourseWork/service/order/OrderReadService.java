package com.example.CourseWork.service.order;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderReadService {
    Page<OrderResponseDto> getAllOrders(Pageable pageable);
    OrderResponseDto getOrderById(Integer id);
    List<OrderResponseDto> getNewOrders();
    List<OrderResponseDto> getActiveOrders();
    Optional<OrderResponseDto> getMyActiveOrder(String userId);
    Page<OrderResponseDto> getOrderHistory(String userId, Pageable pageable);
    OrderResponseDto updateOrderStatus(Integer orderId, OrderStatus newStatus);
}

