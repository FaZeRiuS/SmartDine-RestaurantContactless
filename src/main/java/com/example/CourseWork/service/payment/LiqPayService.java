package com.example.CourseWork.service.payment;

import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.model.Order;

import java.security.SignatureException;

public interface LiqPayService {
    LiqPayCheckoutFormDto prepareCheckout(Order order);

    void validateCallbackSignature(String data, String signature) throws SignatureException;

    LiqPayCallbackDto decodeCallbackData(String data);
}

