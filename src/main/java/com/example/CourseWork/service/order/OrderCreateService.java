package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;

public interface OrderCreateService {
    OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber);
}

