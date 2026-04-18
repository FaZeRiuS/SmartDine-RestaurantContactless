package com.example.CourseWork.controller;

import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.service.payment.LiqPayService;
import com.example.CourseWork.service.payment.PaymentCallbackService;
import com.example.CourseWork.service.payment.PaymentCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@SuppressWarnings("null")
class PaymentControllerTest extends BaseControllerTest {

    @MockitoBean
    private LiqPayService liqPayService;

    @MockitoBean
    private PaymentCallbackService paymentCallbackService;

    @MockitoBean
    private PaymentCheckoutService paymentCheckoutService;

    @Test
    void initPayment_ShouldReturnCheckoutView() throws Exception {
        // Arrange
        LiqPayCheckoutFormDto form = new LiqPayCheckoutFormDto(
                "https://liqpay.ua/api/3/checkout",
                "some-data",
                "some-sig",
                "order_1_123456");
        when(paymentCheckoutService.prepareCheckout(1)).thenReturn(form);

        // Act & Assert
        mockMvc.perform(post("/api/payment/init")
                .with(withUser("user-123", "CUSTOMER"))
                .with(csrf())
                .param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/liqpay-checkout"))
                .andExpect(model().attributeExists("actionUrl", "data", "signature"));
    }

    @Test
    void initPayment_ShouldDenyNonOwner() throws Exception {
        // Arrange
        // Act & Assert
        // User is "other-user", but order belongs to "user-123"
        when(paymentCheckoutService.prepareCheckout(1))
                .thenThrow(new com.example.CourseWork.exception.BadRequestException("Request failed"));

        mockMvc.perform(post("/api/payment/init")
                .with(withUser("other-user", "CUSTOMER"))
                .with(csrf())
                .param("orderId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_ShouldProcessSuccessPayment() throws Exception {
        // Arrange
        LiqPayCallbackDto callback = new LiqPayCallbackDto();
        callback.setOrderId("order_100_123456");
        callback.setStatus("success");
        when(liqPayService.decodeCallbackData(anyString())).thenReturn(callback);

        // Act & Assert
        mockMvc.perform(post("/api/payment/callback")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("data", "valid-data")
                .param("signature", "valid-sig"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(paymentCallbackService).handleCallbackSuccess(any());
        verify(paymentCallbackService).publishOrderUpdateToUser(100);
    }

    @Test
    void callback_ShouldRejectInvalidSignature() throws Exception {
        // Arrange
        doThrow(new java.security.SignatureException("Invalid signature"))
                .when(liqPayService).validateCallbackSignature(eq("bad-data"), eq("bad-sig"));

        // Act & Assert
        mockMvc.perform(post("/api/payment/callback")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("data", "bad-data")
                .param("signature", "bad-sig"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid signature"));
    }
}
