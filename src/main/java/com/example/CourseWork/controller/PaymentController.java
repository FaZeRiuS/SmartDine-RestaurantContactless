package com.example.CourseWork.controller;

import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.service.payment.LiqPayService;
import com.example.CourseWork.service.payment.PaymentCallbackService;
import com.example.CourseWork.service.payment.PaymentCheckoutService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SignatureException;
import com.example.CourseWork.util.LiqPayUtil;

@Controller
@RequestMapping
public class PaymentController {

    private final LiqPayService liqPayService;
    private final PaymentCallbackService paymentCallbackService;
    private final PaymentCheckoutService paymentCheckoutService;

    public PaymentController(
            LiqPayService liqPayService,
            PaymentCallbackService paymentCallbackService,
            PaymentCheckoutService paymentCheckoutService
    ) {
        this.liqPayService = liqPayService;
        this.paymentCallbackService = paymentCallbackService;
        this.paymentCheckoutService = paymentCheckoutService;
    }

    @GetMapping("/payment/result")
    public String paymentResult(@RequestParam("orderId") Integer orderId) {
        // We just redirect to the home page. 
        // The active order widget (js/cart.js) picks up status updates via SSE (sse.js) or its own refresh hooks.
        return "redirect:/?paymentResult=success&orderId=" + orderId;
    }

    @PostMapping("/api/payment/init")
    public String initPayment(@RequestParam("orderId") Integer orderId, Model model) {
        LiqPayCheckoutFormDto form = paymentCheckoutService.prepareCheckout(orderId);
        if (form == null) {
            throw new BadRequestException("Could not initiate payment form");
        }
        model.addAttribute("actionUrl", form.getActionUrl());
        model.addAttribute("data", form.getData());
        model.addAttribute("signature", form.getSignature());
        model.addAttribute("liqpayOrderId", form.getLiqpayOrderId());

        return "payment/liqpay-checkout";
    }

    /**
     * API-friendly checkout endpoint.
     * Returns LiqPay checkout parameters as JSON so frontends can render a form client-side.
     */
    @GetMapping("/api/payment/checkout")
    public ResponseEntity<LiqPayCheckoutFormDto> checkout(@RequestParam("orderId") Integer orderId) {
        return ResponseEntity.ok(paymentCheckoutService.prepareCheckout(orderId));
    }

    @PostMapping(
            value = "/api/payment/callback",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
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

        paymentCallbackService.handleCallbackSuccess(callback);

        Integer dbOrderId = LiqPayUtil.extractDbOrderId(callback.getOrderId());
        paymentCallbackService.publishOrderUpdateToUser(dbOrderId);

        return ResponseEntity.ok("OK");
    }
}
