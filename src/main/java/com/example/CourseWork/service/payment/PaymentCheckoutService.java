package com.example.CourseWork.service.payment;

import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;

public interface PaymentCheckoutService {
    LiqPayCheckoutFormDto prepareCheckout(Integer orderId);
}

