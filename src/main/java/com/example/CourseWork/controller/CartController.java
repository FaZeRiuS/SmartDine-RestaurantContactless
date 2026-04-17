package com.example.CourseWork.controller;

import com.example.CourseWork.dto.CartItemDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.exception.ConflictException;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart() {
        return ResponseEntity.ok(cartService.getCartByUserId(currentUserIdentity.currentUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItemToCart(@Valid @RequestBody CartItemDto itemDto) {
        String userId = currentUserIdentity.currentUserId();
        Optional<OrderResponseDto> active = orderService.getMyActiveOrder(userId);
        if (active.isPresent() && active.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new ConflictException("Активне замовлення вже оплачене — додавання страв тимчасово недоступне");
        }
        return ResponseEntity.ok(cartService.addItemToCart(userId, itemDto));
    }

    @PutMapping("/items/{itemId}/quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Integer itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(currentUserIdentity.currentUserId(), itemId, quantity));
    }

    @PutMapping("/items/{itemId}/special-request")
    public ResponseEntity<CartResponseDto> updateCartItemSpecialRequest(
            @PathVariable Integer itemId,
            @RequestBody(required = false) String specialRequest) {
        return ResponseEntity.ok(cartService.updateCartItemSpecialRequest(currentUserIdentity.currentUserId(), itemId, specialRequest == null ? "" : specialRequest));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(@PathVariable Integer itemId) {
        return ResponseEntity.ok(cartService.removeCartItem(currentUserIdentity.currentUserId(), itemId));
    }
}
