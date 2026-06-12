package com.example.CourseWork.service.security.impl;

import com.example.CourseWork.service.security.HmacSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
public class HmacSignatureServiceImpl implements HmacSignatureService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private final String secretKey;

    public HmacSignatureServiceImpl(
            @Value("${app.security.qr.hmac-secret}") String secretKey,
            Environment env
    ) {
        boolean isTest = Arrays.asList(env.getActiveProfiles()).contains("test");
        if ((secretKey == null || secretKey.isBlank() || "default-unsafe-secret".equals(secretKey)) && !isTest) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: app.security.qr.hmac-secret is missing, empty, or insecure.");
        }
        this.secretKey = (secretKey == null || secretKey.isBlank()) ? "default-unsafe-secret" : secretKey;
    }

    @Override
    public String signTableNumber(int tableNumber) {
        return calculateHmac("table=" + tableNumber);
    }

    @Override
    public boolean verifyTableNumber(int tableNumber, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expectedSignature = calculateHmac("table=" + tableNumber);
        
        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Use Base64 URL safe encoding without padding so it's clean in the URL
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to calculate HMAC signature", e);
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }
}
