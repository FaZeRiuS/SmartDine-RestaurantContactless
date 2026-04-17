package com.example.CourseWork.service;

import com.example.CourseWork.dto.LiqPayCallbackDto;

public interface PaymentCallbackService {
    /**
     * Handles LiqPay callback (signature already validated).
     * Must be idempotent.
     */
    void handleCallbackSuccess(LiqPayCallbackDto callback);

    /**
     * Loads the order with lines and dishes inside a transaction and pushes the snapshot to the owner via SSE.
     * Required when OSIV is disabled — mapping must not run on a detached {@code Order}.
     */
    void publishOrderUpdateToUser(Integer dbOrderId);
}

