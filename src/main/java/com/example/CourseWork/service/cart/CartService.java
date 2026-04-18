package com.example.CourseWork.service.cart;

import com.example.CourseWork.dto.cart.CartItemDto;
import com.example.CourseWork.dto.cart.CartResponseDto;

public interface CartService {
    CartResponseDto getCartByUserId(String userId);
    CartResponseDto addItemToCart(String userId, CartItemDto itemDto);
    CartResponseDto updateCartItemQuantity(String userId, Integer itemId, Integer quantity);
    CartResponseDto updateCartItemSpecialRequest(String userId, Integer itemId, String specialRequest);
    CartResponseDto removeCartItem(String userId, Integer itemId);
}
