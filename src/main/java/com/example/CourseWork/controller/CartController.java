package com.example.CourseWork.controller;

import com.example.CourseWork.dto.CartItemDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.util.KeycloakUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart() {
        return ResponseEntity.ok(cartService.getCartByUserId(KeycloakUtil.getCurrentUser().getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItemToCart(@RequestBody CartItemDto itemDto) {
        return ResponseEntity.ok(cartService.addItemToCart(KeycloakUtil.getCurrentUser().getId(), itemDto));
    }

    @PutMapping("/items/{itemId}/quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Integer itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(KeycloakUtil.getCurrentUser().getId(), itemId, quantity));
    }

    @PutMapping("/items/{itemId}/special-request")
    public ResponseEntity<CartResponseDto> updateCartItemSpecialRequest(
            @PathVariable Integer itemId,
            @RequestBody(required = false) String specialRequest) {
        return ResponseEntity.ok(cartService.updateCartItemSpecialRequest(KeycloakUtil.getCurrentUser().getId(), itemId, specialRequest == null ? "" : specialRequest));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(@PathVariable Integer itemId) {
        return ResponseEntity.ok(cartService.removeCartItem(KeycloakUtil.getCurrentUser().getId(), itemId));
    }
}
