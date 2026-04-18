package com.example.CourseWork.controller;

import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.order.OrderLoyaltyService;
import com.example.CourseWork.service.order.OrderReviewService;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.service.order.OrderTipService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@SuppressWarnings("null")
class OrderControllerTest extends BaseControllerTest {

    @MockitoBean private OrderService orderService;
    @MockitoBean private OrderLoyaltyService orderLoyaltyService;
    @MockitoBean private OrderReviewService orderReviewService;
    @MockitoBean private OrderTipService orderTipService;

    @Test
    void getAllOrders_ShouldRequireAdministrator() throws Exception {
        // Arrange
        when(orderService.getAllOrders(any())).thenReturn(new PageImpl<>(List.of(new OrderResponseDto())));

        // Act & Assert - Forbidden for Customer
        mockMvc.perform(get("/api/orders")
                        .with(withUser("cust-1", "CUSTOMER")))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Administrator
        mockMvc.perform(get("/api/orders")
                        .with(withUser("admin-1", "ADMINISTRATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderHistory_ShouldRequireCustomer() throws Exception {
        // Arrange
        when(orderService.getOrderHistory(eq("cust-1"), any())).thenReturn(new PageImpl<>(List.of(new OrderResponseDto())));

        // Act & Assert - Forbidden for Chef (he sees active orders, not history of specific user)
        mockMvc.perform(get("/api/orders/history")
                        .with(withUser("chef-1", "CHEF")))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Customer
        mockMvc.perform(get("/api/orders/history")
                        .with(withUser("cust-1", "CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void updateOrderStatus_ShouldRequireStaff() throws Exception {
        // Arrange
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1);
        response.setStatus(com.example.CourseWork.model.OrderStatus.PREPARING);
        when(orderService.updateOrderStatus(eq(1), eq(com.example.CourseWork.model.OrderStatus.PREPARING))).thenReturn(response);

        // Act & Assert - Forbidden for Regular Customer
        mockMvc.perform(put("/api/orders/1/status")
                        .with(withUser("cust-1", "CUSTOMER"))
                        .with(csrf())
                        .param("newStatus", "PREPARING"))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Chef
        mockMvc.perform(put("/api/orders/1/status")
                        .with(withUser("chef-1", "CHEF"))
                        .with(csrf())
                        .param("newStatus", "PREPARING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void createOrder_ShouldWorkForAuthenticatedUser() throws Exception {
        // Arrange
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1);
        when(orderService.createOrder(eq("user-1"), any(OrderRequestDto.class), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(withUser("user-1", "CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"dishId\": 1, \"quantity\": 2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void callWaiter_ShouldWorkForAnyUser() throws Exception {
        // Arrange
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1);
        when(orderService.callWaiter(1)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders/1/call-waiter")
                        .with(withUser("user-1", "CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void dismissWaiterCall_ShouldRequireWaiterOrAdmin() throws Exception {
        // Arrange
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1);
        when(orderService.dismissWaiterCall(1)).thenReturn(response);

        // Act & Assert - Forbidden for Customer
        mockMvc.perform(delete("/api/orders/1/call-waiter")
                        .with(withUser("cust-1", "CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Act & Assert - Forbidden for Chef
        mockMvc.perform(delete("/api/orders/1/call-waiter")
                        .with(withUser("chef-1", "CHEF"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Waiter
        mockMvc.perform(delete("/api/orders/1/call-waiter")
                        .with(withUser("waiter-1", "WAITER"))
                        .with(csrf()))
                .andExpect(status().isOk());

        // Act & Assert - OK for Admin
        mockMvc.perform(delete("/api/orders/1/call-waiter")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
