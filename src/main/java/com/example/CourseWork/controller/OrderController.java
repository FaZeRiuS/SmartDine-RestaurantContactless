package com.example.CourseWork.controller;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.ApplyLoyaltyDto;
import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.dto.OrderReviewRequestDto;
import com.example.CourseWork.dto.PaymentRequestDto;
import com.example.CourseWork.dto.TipDto;
import com.example.CourseWork.service.OrderLoyaltyService;
import com.example.CourseWork.service.OrderReviewService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.OrderTipService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderLoyaltyService orderLoyaltyService;
    private final OrderReviewService orderReviewService;
    private final OrderTipService orderTipService;
    private final CurrentUserIdentity currentUserIdentity;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto dto, HttpSession session) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        return ResponseEntity.ok(orderService.createOrder(currentUserIdentity.currentUserId(), dto, tableNumber));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('CHEF', 'WAITER')")
    public ResponseEntity<List<OrderResponseDto>> getNewOrders() {
        return ResponseEntity.ok(orderService.getNewOrders());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF', 'WAITER')")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @GetMapping("/my-active")
    public ResponseEntity<OrderResponseDto> getMyActiveOrder() {
        Optional<OrderResponseDto> order = orderService.getMyActiveOrder(currentUserIdentity.currentUserId());
        return order.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponseDto>> getOrderHistory(Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrderHistory(currentUserIdentity.currentUserId(), pageable));
    }

    @PostMapping("/confirm")
    public ResponseEntity<OrderResponseDto> confirmOrderFromCart(HttpSession session) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        return ResponseEntity.ok(orderService.confirmOrderFromCart(currentUserIdentity.currentUserId(), tableNumber));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF', 'WAITER')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus newStatus) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, newStatus));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponseDto> addItemsToOrder(
            @PathVariable Integer id,
            @Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(orderService.addItemsToOrder(id, currentUserIdentity.currentUserId(), dto));
    }

    @PutMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<OrderResponseDto> updateOrderItemQuantity(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(orderService.updateOrderItemQuantity(orderId, currentUserIdentity.currentUserId(), itemId, quantity));
    }

    @PutMapping("/{orderId}/items/{itemId}/special-request")
    public ResponseEntity<OrderResponseDto> updateOrderItemSpecialRequest(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @RequestBody(required = false) String specialRequest) {
        return ResponseEntity.ok(orderService.updateOrderItemSpecialRequest(orderId, currentUserIdentity.currentUserId(), itemId, specialRequest == null ? "" : specialRequest));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponseDto> removeOrderItem(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId) {
        return ResponseEntity.ok(orderService.removeOrderItem(orderId, currentUserIdentity.currentUserId(), itemId));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponseDto> payOrder(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentRequestDto dto) {
        String paymentMethodId = dto != null && dto.getPaymentMethodId() != null && !dto.getPaymentMethodId().isEmpty() 
                                 ? dto.getPaymentMethodId() : "pm_card_visa";
        return ResponseEntity.ok(orderService.payOrder(id, currentUserIdentity.currentUserId(), paymentMethodId));
    }

    @PostMapping("/{id}/loyalty/apply")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponseDto> applyLoyaltyCoverage(
            @PathVariable Integer id,
            @RequestBody(required = false) ApplyLoyaltyDto dto
    ) {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for this operation");
        return ResponseEntity.ok(orderLoyaltyService.applyCoverage(id, userId, dto != null ? dto.getAmount() : null));
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> submitReview(
            @PathVariable Integer id,
            @Valid @RequestBody OrderReviewRequestDto dto
    ) {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for this operation");
        orderReviewService.submitReview(id, userId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/tip")
    public ResponseEntity<OrderResponseDto> setTip(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) TipDto dto
    ) {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for this operation");
        return ResponseEntity.ok(orderTipService.setTip(id, userId, dto != null ? dto.getAmount() : null));
    }

    @PostMapping("/{id}/call-waiter")
    public ResponseEntity<OrderResponseDto> callWaiter(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.callWaiter(id));
    }

    @DeleteMapping("/{id}/call-waiter")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'WAITER')")
    public ResponseEntity<OrderResponseDto> dismissWaiterCall(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.dismissWaiterCall(id));
    }

}
