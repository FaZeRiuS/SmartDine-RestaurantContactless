package com.example.CourseWork.service;

public interface PaymentService {
    boolean processPayment(float amount, String orderId) throws Exception;
}
