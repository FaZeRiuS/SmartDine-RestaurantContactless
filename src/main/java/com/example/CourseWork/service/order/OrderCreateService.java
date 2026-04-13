package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;

public interface OrderCreateService {
    OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber);
}

