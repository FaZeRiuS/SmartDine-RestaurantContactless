package com.example.CourseWork.service;

import com.example.CourseWork.dto.OrderResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderLoyaltyService {
    OrderResponseDto applyCoverage(Integer orderId, UUID userId, BigDecimal desiredAmount);
}

