package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderTipService {
    OrderResponseDto setTip(Integer orderId, UUID userId, BigDecimal amount);
}

