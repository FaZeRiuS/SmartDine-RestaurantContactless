package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.OrderResponseDto;

public interface OrderWaiterService {
    OrderResponseDto callWaiter(Integer orderId);
    OrderResponseDto dismissWaiterCall(Integer orderId);
}

