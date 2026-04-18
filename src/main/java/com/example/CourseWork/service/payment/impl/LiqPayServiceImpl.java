package com.example.CourseWork.service.payment.impl;

import com.example.CourseWork.config.LiqPayConfig;
import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.service.payment.LiqPayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LiqPayServiceImpl implements LiqPayService {

    private static final String CHECKOUT_URL = "https://www.liqpay.ua/api/3/checkout";
    private static final String VERSION = "3";
    private static final String CURRENCY = "UAH";

    private final LiqPayConfig liqPayConfig;
    private final ObjectMapper objectMapper;

    public LiqPayServiceImpl(LiqPayConfig liqPayConfig, ObjectMapper objectMapper) {
        this.liqPayConfig = liqPayConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public LiqPayCheckoutFormDto prepareCheckout(Order order) {
        String liqpayOrderId = "order_" + order.getId() + "_" + System.currentTimeMillis();

        BigDecimal total = (order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = (order.getLoyaltyDiscount() == null ? BigDecimal.ZERO : order.getLoyaltyDiscount())
                .setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(total) > 0) discount = total;
        BigDecimal tip = (order.getTipAmount() == null ? BigDecimal.ZERO : order.getTipAmount())
                .setScale(2, RoundingMode.HALF_UP);
        if (tip.compareTo(BigDecimal.ZERO) < 0) tip = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal payable = total.subtract(discount).add(tip).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", VERSION);
        payload.put("public_key", liqPayConfig.getPublicKey());
        payload.put("action", "pay");
        payload.put("amount", payable);
        payload.put("currency", CURRENCY);
        payload.put("description", "Order #" + order.getId() + " payment");
        payload.put("order_id", liqpayOrderId);
        payload.put("result_url", liqPayConfig.getPublicBaseUrl() + "/payment/result?orderId=" + order.getId());
        payload.put("server_url", liqPayConfig.getPublicBaseUrl() + "/api/payment/callback");
        payload.put("sandbox", liqPayConfig.getSandbox());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build LiqPay JSON payload", e);
        }

        String data = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        String signature = buildSignature(data);

        return new LiqPayCheckoutFormDto(CHECKOUT_URL, data, signature, liqpayOrderId);
    }

    @Override
    public void validateCallbackSignature(String data, String signature) throws SignatureException {
        if (data == null || data.isBlank()) {
            throw new SignatureException("Missing data");
        }
        if (signature == null || signature.isBlank()) {
            throw new SignatureException("Missing signature");
        }

        String expected = buildSignature(data);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signature.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new SignatureException("Invalid LiqPay signature");
        }
    }

    @Override
    public LiqPayCallbackDto decodeCallbackData(String data) {
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, LiqPayCallbackDto.class);
        } catch (Exception e) {
            throw new BadRequestException(ErrorMessages.INVALID_LIQPAY_CALLBACK_DATA);
        }
    }

    private String buildSignature(String data) {
        try {
            String input = liqPayConfig.getPrivateKey() + data + liqPayConfig.getPrivateKey();
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build LiqPay signature", e);
        }
    }
}

