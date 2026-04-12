package com.example.CourseWork.controller;

import com.example.CourseWork.dto.CartItemDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@SuppressWarnings("null")
class CartControllerTest extends BaseControllerTest {

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_ShouldReturnCartForCurrentUser() throws Exception {
        // Arrange
        CartResponseDto response = new CartResponseDto();
        response.setItems(List.of());
        when(cartService.getCartByUserId("user-1")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/cart")
                        .with(withUser("user-1", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void addItemToCart_ShouldWorkForAuthenticatedUser() throws Exception {
        // Arrange
        CartResponseDto response = new CartResponseDto();
        when(cartService.addItemToCart(eq("user-1"), any(CartItemDto.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/cart/items")
                        .with(withUser("user-1", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishId\": 1, \"quantity\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void removeCartItem_ShouldWorkForAuthenticatedUser() throws Exception {
        // Arrange
        CartResponseDto response = new CartResponseDto();
        when(cartService.removeCartItem("user-1", 100)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/items/100")
                        .with(withUser("user-1", "CUSTOMER")))
                .andExpect(status().isOk());
    }
}
