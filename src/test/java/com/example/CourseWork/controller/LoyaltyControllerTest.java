package com.example.CourseWork.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoyaltyController.class)
@SuppressWarnings("null")
class LoyaltyControllerTest extends BaseControllerTest {

    @Test
    void getBalance_ShouldReturnBalanceForCustomer() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("150.50");
        when(loyaltyService.getBalance(any())).thenReturn(balance);

        // Act & Assert
        mockMvc.perform(get("/api/loyalty/balance")
                        .with(withUser(userId.toString(), "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.5));
    }

    @Test
    void getBalance_ShouldDenyUnauthorized() throws Exception {
        // Act & Assert - No user
        mockMvc.perform(get("/api/loyalty/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_ShouldReturnProgresForCustomer() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(loyaltyService.getBalance(any())).thenReturn(new BigDecimal("100.00"));
        when(loyaltyService.getSuccessfulOrdersCount(any())).thenReturn(5L);
        when(loyaltyService.resolveCashbackRate(any())).thenReturn(new BigDecimal("0.02"));

        // Act & Assert
        mockMvc.perform(get("/api/loyalty/summary")
                        .with(withUser(userId.toString(), "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashbackRate").value(0.02))
                .andExpect(jsonPath("$.ordersToNextRate").value(5))
                .andExpect(jsonPath("$.nextRate").value(0.03));
    }

    @Test
    void getSummary_ShouldShowMaxRateForFrequentCustomer() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(loyaltyService.getBalance(any())).thenReturn(new BigDecimal("1000.00"));
        when(loyaltyService.getSuccessfulOrdersCount(any())).thenReturn(150L); // Over 100
        when(loyaltyService.resolveCashbackRate(any())).thenReturn(new BigDecimal("0.05"));

        // Act & Assert
        mockMvc.perform(get("/api/loyalty/summary")
                        .with(withUser(userId.toString(), "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashbackRate").value(0.05))
                .andExpect(jsonPath("$.ordersToNextRate").value(0));
    }
}
