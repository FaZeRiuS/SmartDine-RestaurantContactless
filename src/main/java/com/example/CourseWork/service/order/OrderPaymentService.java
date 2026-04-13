package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.OrderResponseDto;

public interface OrderPaymentService {
    OrderResponseDto payOrder(Integer orderId, String userId, String paymentMethodId);
}

