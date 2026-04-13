package com.example.CourseWork.service;

import com.example.CourseWork.dto.LiqPayCallbackDto;

public interface PaymentCallbackService {
    /**
     * Handles LiqPay callback (signature already validated).
     * Must be idempotent.
     */
    void handleCallbackSuccess(LiqPayCallbackDto callback);
}

