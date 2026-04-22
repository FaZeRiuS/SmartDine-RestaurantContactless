package com.example.CourseWork.service.order;

import com.example.CourseWork.dto.order.OrderReviewRequestDto;

public interface OrderReviewService {
    void submitReview(Integer orderId, String userId, OrderReviewRequestDto dto);
}

