package com.example.CourseWork.service.impl;

import com.example.CourseWork.service.PaymentService;
import com.liqpay.LiqPay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Value("${liqpay.public.key}")
    private String publicKey;

    @Value("${liqpay.private.key}")
    private String privateKey;

    @Value("${liqpay.sandbox:0}")
    private Integer sandbox;

    @Override
    public boolean processPayment(float amount, String orderId) throws Exception {
        LiqPay liqpay = getLiqPay();

        Map<String, String> params = new HashMap<>();
        params.put("action", "pay");
        params.put("amount", String.valueOf(amount));
        params.put("currency", "UAH");
        params.put("description", "Order #" + orderId + " payment");
        params.put("order_id", "order_" + orderId + "_" + System.currentTimeMillis());
        params.put("version", "3");
        params.put("sandbox", String.valueOf(sandbox != null ? sandbox : 0));

        Map<String, Object> res = liqpay.api("request", params);

        Object status = res.get("status");
        log.info("LiqPay payment response for order {}: status={}", orderId, status);

        return "success".equals(status) || "sandbox".equals(status) || "wait_compensation".equals(status);
    }

    protected LiqPay getLiqPay() {
        return new LiqPay(publicKey, privateKey);
    }
}
