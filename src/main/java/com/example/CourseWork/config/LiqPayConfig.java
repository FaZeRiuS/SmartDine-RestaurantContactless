package com.example.CourseWork.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiqPayConfig {

    @Value("${liqpay.public.key}")
    private String publicKey;

    @Value("${liqpay.private.key}")
    private String privateKey;

    /**
     * LiqPay expects sandbox=1 for test mode.
     */
    @Value("${liqpay.sandbox:1}")
    private Integer sandbox;

    @Value("${app.public.base-url}")
    private String publicBaseUrl;

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Integer getSandbox() {
        return sandbox;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }
}

