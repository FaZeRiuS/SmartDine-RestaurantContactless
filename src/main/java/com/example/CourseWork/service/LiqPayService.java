package com.example.CourseWork.service;

import com.example.CourseWork.dto.LiqPayCallbackDto;
import com.example.CourseWork.dto.LiqPayCheckoutFormDto;
import com.example.CourseWork.model.Order;

import java.security.SignatureException;

public interface LiqPayService {
    LiqPayCheckoutFormDto prepareCheckout(Order order);

    void validateCallbackSignature(String data, String signature) throws SignatureException;

    LiqPayCallbackDto decodeCallbackData(String data);
}

