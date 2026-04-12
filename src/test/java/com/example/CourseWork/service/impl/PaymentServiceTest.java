package com.example.CourseWork.service.impl;

import com.liqpay.LiqPay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private LiqPay liqPay;

    @Spy
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "publicKey", "test-pub");
        ReflectionTestUtils.setField(paymentService, "privateKey", "test-priv");
        ReflectionTestUtils.setField(paymentService, "sandbox", 1);
        
        // Ensure spy returns the mock liqPay
        doReturn(liqPay).when(paymentService).getLiqPay();
    }

    @Test
    void processPayment_ShouldReturnTrue_WhenStatusIsSuccess() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        when(liqPay.api(eq("request"), anyMap())).thenReturn(response);

        // Act
        boolean result = paymentService.processPayment(100.0f, "ORDER1");

        // Assert
        assertTrue(result);
        verify(liqPay).api(eq("request"), anyMap());
    }

    @Test
    void processPayment_ShouldReturnTrue_WhenStatusIsSandbox() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("status", "sandbox");
        when(liqPay.api(eq("request"), anyMap())).thenReturn(response);

        // Act
        boolean result = paymentService.processPayment(50.0f, "ORDER2");

        // Assert
        assertTrue(result);
    }

    @Test
    void processPayment_ShouldReturnFalse_WhenStatusIsFailure() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failure");
        when(liqPay.api(eq("request"), anyMap())).thenReturn(response);

        // Act
        boolean result = paymentService.processPayment(10.0f, "ORDER3");

        // Assert
        assertFalse(result);
    }
}
