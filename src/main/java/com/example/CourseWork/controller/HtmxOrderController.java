package com.example.CourseWork.controller;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.OrderLoyaltyService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.OrderTipService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/htmx/orders")
@RequiredArgsConstructor
public class HtmxOrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final OrderLoyaltyService orderLoyaltyService;
    private final OrderTipService orderTipService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping("/active-panel")
    public String activePanel(
            @RequestParam(defaultValue = "false") boolean home,
            Model model,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return renderActiveOrderPanel(home, model);
    }

    @GetMapping("/{id}/review-modal")
    public String reviewModal(@PathVariable Integer id, Model model) {
        OrderResponseDto order = orderService.getOrderById(id);
        boolean canReview = order.getPaymentStatus() == PaymentStatus.SUCCESS
                && (order.getStatus() == OrderStatus.READY || order.getStatus() == OrderStatus.COMPLETED)
                && order.getServiceRating() == null;
        if (!canReview) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("order", order);
        return "fragments/order-review-modal :: reviewOverlay";
    }

    @PostMapping("/confirm")
    public String confirmFromCart(HttpSession session, HttpServletResponse response, Model model) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        orderService.confirmOrderFromCart(currentUserIdentity.currentUserId(), tableNumber);

        response.setHeader("HX-Refresh", "true");

        CartResponseDto cart = cartService.getCartByUserId(currentUserIdentity.currentUserId());
        model.addAttribute("cartCount", cartCount(cart));
        model.addAttribute("message", "Замовлення оформлено! Очікуйте оновлення статусу.");
        model.addAttribute("toastType", "toast-success");
        return "fragments/cart :: toast";
    }

    @PutMapping("/{orderId}/items/{itemId}/quantity")
    public String updateOrderItemQuantity(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        orderService.updateOrderItemQuantity(orderId, currentUserIdentity.currentUserId(), itemId, quantity);
        return renderActiveOrderPanel(home, model);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public String removeOrderItem(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        orderService.removeOrderItem(orderId, currentUserIdentity.currentUserId(), itemId);
        return renderActiveOrderPanel(home, model);
    }

    @PutMapping("/{orderId}/items/{itemId}/special-request")
    public String updateOrderItemSpecialRequest(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @RequestParam(required = false, defaultValue = "") String specialRequest,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        orderService.updateOrderItemSpecialRequest(orderId, currentUserIdentity.currentUserId(), itemId, specialRequest);
        return renderActiveOrderPanel(home, model);
    }

    @PostMapping("/{id}/loyalty/apply")
    public String applyLoyaltyHtmx(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for this operation");
        orderLoyaltyService.applyCoverage(id, userId, amount);
        model.addAttribute("toastMessage", "Бали лояльності застосовано.");
        model.addAttribute("toastType", "toast-success");
        return renderActiveOrderPanel(home, model);
    }

    @PostMapping("/{id}/tip")
    public String setTipHtmx(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for this operation");
        orderTipService.setTip(id, userId, amount);
        model.addAttribute("toastMessage", "Чайові збережено. Дякуємо!");
        model.addAttribute("toastType", "toast-success");
        return renderActiveOrderPanel(home, model);
    }

    @PostMapping("/{id}/call-waiter")
    public String callWaiterHtmx(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean home,
            Model model) {
        orderService.callWaiter(id);
        return renderActiveOrderPanel(home, model);
    }

    private String renderActiveOrderPanel(boolean home, Model model) {
        model.addAttribute("homeActiveOrderWidget", home);
        Optional<OrderResponseDto> opt = orderService.getMyActiveOrder(currentUserIdentity.currentUserId());
        if (opt.isEmpty()) {
            model.addAttribute("activeOrder", null);
            return "fragments/active-order :: panel";
        }
        OrderResponseDto o = opt.get();
        if (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.CANCELLED) {
            model.addAttribute("activeOrder", null);
            return "fragments/active-order :: panel";
        }
        model.addAttribute("activeOrder", o);
        return "fragments/active-order :: panel";
    }

    private static int cartCount(CartResponseDto cart) {
        if (cart == null || cart.getItems() == null) return 0;
        return cart.getItems().stream()
                .filter(i -> i != null && i.getQuantity() != null)
                .mapToInt(i -> i.getQuantity())
                .sum();
    }
}
