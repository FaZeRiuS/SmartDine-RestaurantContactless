package com.example.CourseWork.controller;

import com.example.CourseWork.dto.loyalty.LoyaltyBalanceDto;
import com.example.CourseWork.dto.loyalty.LoyaltySummaryDto;
import com.example.CourseWork.security.CurrentUserIdentity;
import com.example.CourseWork.service.loyalty.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping("/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LoyaltyBalanceDto> getBalance() {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for loyalty");

        LoyaltyBalanceDto dto = new LoyaltyBalanceDto();
        dto.setUserId(userId);
        dto.setBalance(loyaltyService.getBalance(userId));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LoyaltySummaryDto> getSummary() {
        UUID userId = currentUserIdentity.requireCustomerUuid("Customer account is required for loyalty");
        Long count = loyaltyService.getSuccessfulOrdersCount(userId);
        var rate = loyaltyService.resolveCashbackRate(userId);

        LoyaltySummaryDto dto = new LoyaltySummaryDto();
        dto.setUserId(userId);
        dto.setBalance(loyaltyService.getBalance(userId));
        dto.setCashbackRate(rate);
        dto.setSuccessfulOrdersCount(count);

        if (count != null && count < 10) {
            dto.setNextRate(new BigDecimal("0.03"));
            dto.setOrdersToNextRate(10 - count);
        } else if (count != null && count < 100) {
            dto.setNextRate(new BigDecimal("0.05"));
            dto.setOrdersToNextRate(100 - count);
        } else {
            dto.setNextRate(rate);
            dto.setOrdersToNextRate(0L);
        }

        return ResponseEntity.ok(dto);
    }
}

