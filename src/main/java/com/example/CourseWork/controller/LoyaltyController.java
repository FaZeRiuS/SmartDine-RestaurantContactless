package com.example.CourseWork.controller;

import com.example.CourseWork.dto.LoyaltyBalanceDto;
import com.example.CourseWork.dto.LoyaltySummaryDto;
import com.example.CourseWork.service.LoyaltyService;
import com.example.CourseWork.util.KeycloakUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LoyaltyBalanceDto> getBalance() {
        String rawUserId = KeycloakUtil.getCurrentUser().getId();
        UUID userId;
        try {
            userId = UUID.fromString(rawUserId);
        } catch (Exception e) {
            throw new RuntimeException("Invalid user id");
        }

        LoyaltyBalanceDto dto = new LoyaltyBalanceDto();
        dto.setUserId(userId);
        dto.setBalance(loyaltyService.getBalance(userId));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LoyaltySummaryDto> getSummary() {
        UUID userId = UUID.fromString(KeycloakUtil.getCurrentUser().getId());
        Long count = loyaltyService.getSuccessfulOrdersCount(userId);
        var rate = loyaltyService.resolveCashbackRate(userId);

        LoyaltySummaryDto dto = new LoyaltySummaryDto();
        dto.setUserId(userId);
        dto.setBalance(loyaltyService.getBalance(userId));
        dto.setCashbackRate(rate);
        dto.setSuccessfulOrdersCount(count);

        if (count != null && count < 10) {
            dto.setNextRate(new java.math.BigDecimal("0.03"));
            dto.setOrdersToNextRate(10 - count);
        } else if (count != null && count < 100) {
            dto.setNextRate(new java.math.BigDecimal("0.05"));
            dto.setOrdersToNextRate(100 - count);
        } else {
            dto.setNextRate(rate);
            dto.setOrdersToNextRate(0L);
        }

        return ResponseEntity.ok(dto);
    }
}

