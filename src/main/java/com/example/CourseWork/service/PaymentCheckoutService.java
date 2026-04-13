package com.example.CourseWork.service;

import com.example.CourseWork.dto.LiqPayCheckoutFormDto;

public interface PaymentCheckoutService {
    LiqPayCheckoutFormDto prepareCheckout(Integer orderId);
}

