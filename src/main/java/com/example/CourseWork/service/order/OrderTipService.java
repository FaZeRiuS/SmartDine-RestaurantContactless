package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderResponseDto;

import java.math.BigDecimal;

public interface OrderTipService {
    OrderResponseDto setTip(Integer orderId, String userId, BigDecimal amount);
}
