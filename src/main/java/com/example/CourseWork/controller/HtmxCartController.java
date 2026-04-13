package com.example.CourseWork.controller;

import com.example.CourseWork.dto.CartItemDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/htmx/cart")
@RequiredArgsConstructor
public class HtmxCartController {

    private final CartService cartService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping("/widget")
    public String getCartWidget(Model model) {
        CartResponseDto cart = cartService.getCartByUserId(currentUserIdentity.currentUserId());
        model.addAttribute("cart", cart);
        return "fragments/cart :: widget";
    }

    @PostMapping("/items")
    public String addItemToCart(@RequestParam Integer dishId, 
                                @RequestParam(defaultValue = "1") Integer quantity,
                                @RequestParam(required = false, defaultValue = "") String specialRequest,
                                Model model) {
        CartItemDto itemDto = new CartItemDto();
        itemDto.setDishId(dishId);
        itemDto.setQuantity(quantity);
        itemDto.setSpecialRequest(specialRequest);

        CartResponseDto cart = cartService.addItemToCart(currentUserIdentity.currentUserId(), itemDto);
        model.addAttribute("cart", cart);
        
        // Return a toast or updated badge fragment
        return "fragments/cart :: added-toast";
    }

    @DeleteMapping("/items/{itemId}")
    public String removeCartItem(@PathVariable Integer itemId, Model model) {
        CartResponseDto cart = cartService.removeCartItem(currentUserIdentity.currentUserId(), itemId);
        model.addAttribute("cart", cart);
        return "fragments/cart :: content";
    }
}
