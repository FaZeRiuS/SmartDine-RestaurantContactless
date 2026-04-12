package com.example.CourseWork.service;

import com.example.CourseWork.dto.OrderReviewRequestDto;

import java.util.UUID;

public interface OrderReviewService {
    void submitReview(Integer orderId, UUID userId, OrderReviewRequestDto dto);
}

