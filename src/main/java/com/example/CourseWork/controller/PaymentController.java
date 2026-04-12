package com.example.CourseWork.controller;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.LiqPayCallbackDto;
import com.example.CourseWork.dto.LiqPayCheckoutFormDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.LiqPayService;
import com.example.CourseWork.service.LoyaltyService;
import com.example.CourseWork.util.KeycloakUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SignatureException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping
public class PaymentController {

    private static final Pattern LIQPAY_ORDER_ID_PATTERN = Pattern.compile("^order_(\\d+)_\\d+$");

    private final OrderRepository orderRepository;
    private final LiqPayService liqPayService;
    private final OrderMapper orderMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final LoyaltyService loyaltyService;

    public PaymentController(
            OrderRepository orderRepository,
            LiqPayService liqPayService,
            OrderMapper orderMapper,
            SimpMessagingTemplate messagingTemplate,
            LoyaltyService loyaltyService
    ) {
        this.orderRepository = orderRepository;
        this.liqPayService = liqPayService;
        this.orderMapper = orderMapper;
        this.messagingTemplate = messagingTemplate;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/payment/result")
    public String paymentResult(@RequestParam("orderId") Integer orderId) {
        // We just redirect to the home page. 
        // The active order widget (js/cart.js) automatically picks up the updated status via WebSocket or polling.
        return "redirect:/?paymentResult=success&orderId=" + orderId;
    }

    @PostMapping("/api/payment/init")
    @SuppressWarnings("null")
    public String initPayment(@RequestParam("orderId") Integer orderId, Model model) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String currentUserId = KeycloakUtil.getCurrentUser().getId();
        if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }

        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order is already paid");
        }

        LiqPayCheckoutFormDto form = liqPayService.prepareCheckout(order);
        model.addAttribute("actionUrl", form.getActionUrl());
        model.addAttribute("data", form.getData());
        model.addAttribute("signature", form.getSignature());
        model.addAttribute("liqpayOrderId", form.getLiqpayOrderId());

        return "payment/liqpay-checkout";
    }

    @PostMapping(
            value = "/api/payment/callback",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @Transactional
    @SuppressWarnings("null")
    public ResponseEntity<String> callback(
            @RequestParam("data") String data,
            @RequestParam("signature") String signature
    ) throws SignatureException {
        liqPayService.validateCallbackSignature(data, signature);

        LiqPayCallbackDto callback = liqPayService.decodeCallbackData(data);
        if (callback == null || callback.getOrderId() == null || callback.getOrderId().isBlank()) {
            return ResponseEntity.badRequest().body("Missing order_id");
        }

        String status = callback.getStatus() != null ? callback.getStatus() : "";
        boolean isSuccess = "success".equalsIgnoreCase(status) || "sandbox".equalsIgnoreCase(status);
        if (!isSuccess) {
            return ResponseEntity.ok("Ignored status=" + status);
        }

        Integer dbOrderId = extractDbOrderId(callback.getOrderId());
        Order order = orderRepository.findById(dbOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + dbOrderId));

        if (!PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            if (OrderStatus.READY.equals(order.getStatus())) {
                order.setStatus(OrderStatus.COMPLETED);
            }
            orderRepository.save(order);

            earnCashbackIfEligible(order);
        }

        OrderResponseDto response = orderMapper.toResponseDto(order);
        if (order.getUserId() != null) {
            messagingTemplate.convertAndSend("/topic/order-updates/" + order.getUserId(), (Object) response);
        }

        return ResponseEntity.ok("OK");
    }

    private void earnCashbackIfEligible(Order order) {
        if (order == null || order.getUserId() == null || order.getUserId().isBlank()) {
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(order.getUserId());
        } catch (Exception e) {
            // Guest sessions and any non-UUID values are not eligible.
            return;
        }

        String reference = "LIQPAY:order:" + order.getId();
        BigDecimal total = BigDecimal.valueOf(order.getTotalPrice());
        BigDecimal discount = order.getLoyaltyDiscount() != null ? order.getLoyaltyDiscount() : BigDecimal.ZERO;
        BigDecimal orderAmount = total.subtract(discount); // tips are excluded from cashback
        if (orderAmount.compareTo(BigDecimal.ZERO) < 0) {
            orderAmount = BigDecimal.ZERO;
        }
        loyaltyService.earnPointsInternal(userId, orderAmount, reference);
    }

    private Integer extractDbOrderId(String liqpayOrderId) {
        Matcher m = LIQPAY_ORDER_ID_PATTERN.matcher(liqpayOrderId);
        if (!m.matches()) {
            throw new RuntimeException("Invalid order_id format: " + liqpayOrderId);
        }
        return Integer.parseInt(m.group(1));
    }
}

