package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderReviewRequestDto;

import java.util.UUID;

public interface OrderReviewService {
    void submitReview(Integer orderId, UUID userId, OrderReviewRequestDto dto);
}

