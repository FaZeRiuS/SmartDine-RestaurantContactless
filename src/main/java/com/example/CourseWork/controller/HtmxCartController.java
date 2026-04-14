package com.example.CourseWork.controller;

import com.example.CourseWork.dto.*;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/htmx/cart")
@RequiredArgsConstructor
public class HtmxCartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final com.example.CourseWork.service.RecommendationService recommendationService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping("/widget")
    public String getCartWidget(Model model) {
        CartResponseDto cart = cartService.getCartByUserId(currentUserIdentity.currentUserId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cartCount(cart));
        return "fragments/cart :: widget";
    }

    @PostMapping("/items")
    public String addItemToCart(@RequestParam Integer dishId, 
                                @RequestParam(defaultValue = "1") Integer quantity,
                                @RequestParam(required = false, defaultValue = "") String specialRequest,
                                HttpSession session,
                                HttpServletRequest request,
                                Model model) {
        
        String userId = currentUserIdentity.currentUserId();
        Optional<OrderResponseDto> activeOrderOpt = orderService.getMyActiveOrder(userId);
        boolean addedToOrder = false;
        
        if (activeOrderOpt.isPresent()) {
            OrderResponseDto activeOrder = activeOrderOpt.get();
            if (activeOrder.getPaymentStatus() != PaymentStatus.SUCCESS) {
                // Directly add to the UNPAID order
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setDishId(dishId);
                itemDto.setQuantity(quantity);
                itemDto.setSpecialRequest(specialRequest);
                
                OrderRequestDto reqDto = new OrderRequestDto();
                reqDto.setItems(List.of(itemDto));
                
                orderService.addItemsToOrder(activeOrder.getId(), userId, reqDto);
                addedToOrder = true;
            }
        }
        
        List<Integer> existingDishIds;
        if (!addedToOrder) {
            // Add to cart
            CartItemDto itemDto = new CartItemDto();
            itemDto.setDishId(dishId);
            itemDto.setQuantity(quantity);
            itemDto.setSpecialRequest(specialRequest);

            CartResponseDto cart = cartService.addItemToCart(userId, itemDto);
            model.addAttribute("cart", cart);
            existingDishIds = cart.getItems().stream().map(CartItemDetailDto::getDishId).collect(Collectors.toList());
        } else {
            existingDishIds = activeOrderOpt.get().getItems().stream().map(com.example.CourseWork.dto.OrderItemResponseDto::getDishId).collect(Collectors.toList());
        }

        // Recommendation logic (Smart Combo)
        var recommendation = recommendationService.getCrossSellRecommendation(dishId, existingDishIds);
        if (recommendation.isPresent()) {
            model.addAttribute("recommendDish", recommendation.get());
        }

        CartResponseDto cartForBadge = (CartResponseDto) model.getAttribute("cart");
        if (cartForBadge == null) {
            cartForBadge = cartService.getCartByUserId(userId);
        }
        model.addAttribute("cartCount", cartCount(cartForBadge));

        model.addAttribute("message", addedToOrder ? "🛎️ Страву додано до активного замовлення!" : "🛒 Страву додано у кошик!");
        model.addAttribute("toastType", "toast-success");
        if (addedToOrder) {
            model.addAttribute("toastPrimaryHref", homeActiveOrderHref(session, request));
            model.addAttribute("toastPrimaryLabel", "До замовлення");
            model.addAttribute("toastSecondaryLabel", "Перейти до замовлення");
        }
        return "fragments/cart :: toast";
    }

    private static String homeActiveOrderHref(HttpSession session, HttpServletRequest request) {
        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        StringBuilder sb = new StringBuilder(ctx).append("/");
        Integer table = (Integer) session.getAttribute("tableNumber");
        if (table != null) {
            sb.append("?table=").append(table);
        }
        sb.append("#activeOrderContainer");
        return sb.toString();
    }

    @DeleteMapping("/items/{itemId}")
    public String removeCartItem(@PathVariable Integer itemId, Model model) {
        CartResponseDto cart = cartService.removeCartItem(currentUserIdentity.currentUserId(), itemId);
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal(cart));
        model.addAttribute("cartCount", cartCount(cart));
        return "fragments/cart :: content";
    }

    @PutMapping("/items/{itemId}/quantity")
    public String updateCartItemQuantity(
            @PathVariable Integer itemId,
            @RequestParam Integer quantity,
            Model model) {
        CartResponseDto cart = cartService.updateCartItemQuantity(currentUserIdentity.currentUserId(), itemId, quantity);
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal(cart));
        model.addAttribute("cartCount", cartCount(cart));
        return "fragments/cart :: content";
    }

    @GetMapping("/content")
    public String getCartContent(Model model) {
        CartResponseDto cart = cartService.getCartByUserId(currentUserIdentity.currentUserId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal(cart));
        model.addAttribute("cartCount", cartCount(cart));
        return "fragments/cart :: content";
    }

    private static int cartCount(CartResponseDto cart) {
        if (cart == null || cart.getItems() == null) return 0;
        return cart.getItems().stream()
                .filter(i -> i != null && i.getQuantity() != null)
                .mapToInt(CartItemDetailDto::getQuantity)
                .sum();
    }

    private static BigDecimal cartTotal(CartResponseDto cart) {
        if (cart == null || cart.getItems() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .filter(i -> i != null && i.getPrice() != null && i.getQuantity() != null)
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
