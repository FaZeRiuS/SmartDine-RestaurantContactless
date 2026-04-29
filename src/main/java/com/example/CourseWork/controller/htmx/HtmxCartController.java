package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.dto.cart.CartItemDetailDto;
import com.example.CourseWork.dto.cart.CartItemDto;
import com.example.CourseWork.dto.cart.CartResponseDto;
import com.example.CourseWork.dto.order.OrderItemDto;
import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.cart.CartService;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.exception.ConflictException;
import com.example.CourseWork.security.CurrentUserIdentity;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.CourseWork.service.recommendation.RecommendationService;

@Controller
@RequestMapping("/htmx/cart")
@RequiredArgsConstructor
public class HtmxCartController {

    private static final String SESSION_KEY_SMART_COMBO_LAST_SHOWN_AT = "smartComboLastShownAtMs";
    // Reduce spam: show smart combo offer at most once per cooldown window.
    private static final long SMART_COMBO_COOLDOWN_MS = 3 * 60 * 1000L; // 3 minutes

    private final CartService cartService;
    private final OrderService orderService;
    private final RecommendationService recommendationService;
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
                                Model model,
                                HttpSession session) {
        
        String userId = currentUserIdentity.currentUserId();
        Optional<OrderResponseDto> activeOrderOpt = orderService.getMyActiveOrder(userId);
        boolean addedToOrder = false;
        
        if (activeOrderOpt.isPresent()) {
            OrderResponseDto activeOrder = activeOrderOpt.get();
            if (activeOrder.getPaymentStatus() == PaymentStatus.SUCCESS) {
                throw new ConflictException("Активне замовлення вже оплачене — додавання страв тимчасово недоступне");
            }
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
            existingDishIds = activeOrderOpt.get().getItems().stream().map(com.example.CourseWork.dto.order.OrderItemResponseDto::getDishId).collect(Collectors.toList());
        }

        // Recommendation logic (Smart Combo)
        if (shouldShowSmartCombo(session)) {
            var recommendation = recommendationService.getCrossSellRecommendation(dishId, existingDishIds);
            if (recommendation.isPresent()) {
                model.addAttribute("recommendDish", recommendation.get());
                markSmartComboShown(session);
            }
        }

        CartResponseDto cartForBadge = (CartResponseDto) model.getAttribute("cart");
        if (cartForBadge == null) {
            cartForBadge = cartService.getCartByUserId(userId);
        }
        model.addAttribute("cartCount", cartCount(cartForBadge));

        model.addAttribute("message", addedToOrder ? "🛎️ Страву додано до активного замовлення!" : "🛒 Страву додано у кошик!");
        model.addAttribute("toastType", "toast-success");
        return "fragments/cart :: toastAndSync";
    }

    private static boolean shouldShowSmartCombo(HttpSession session) {
        if (session == null) return true;
        Object v = session.getAttribute(SESSION_KEY_SMART_COMBO_LAST_SHOWN_AT);
        if (v instanceof Long last) {
            return (System.currentTimeMillis() - last) >= SMART_COMBO_COOLDOWN_MS;
        }
        if (v instanceof Number n) {
            long last = n.longValue();
            return (System.currentTimeMillis() - last) >= SMART_COMBO_COOLDOWN_MS;
        }
        return true;
    }

    private static void markSmartComboShown(HttpSession session) {
        if (session == null) return;
        session.setAttribute(SESSION_KEY_SMART_COMBO_LAST_SHOWN_AT, System.currentTimeMillis());
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

    @PutMapping("/items/{itemId}/special-request")
    public String updateCartItemSpecialRequest(
            @PathVariable Integer itemId,
            @RequestParam(required = false, defaultValue = "") String specialRequest,
            Model model) {
        CartResponseDto cart = cartService.updateCartItemSpecialRequest(
                currentUserIdentity.currentUserId(),
                itemId,
                specialRequest
        );
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
