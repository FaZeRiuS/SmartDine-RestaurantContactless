package com.example.CourseWork.service.loyalty;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoyaltyService {
    void earnPoints(UUID userId, BigDecimal orderAmount);

    void spendPoints(UUID userId, BigDecimal amount);

    BigDecimal getBalance(UUID userId);

    BigDecimal resolveCashbackRate(UUID userId);

    Long getSuccessfulOrdersCount(UUID userId);

    void earnPointsInternal(UUID userId, BigDecimal orderAmount, String reference);

    void spendPointsInternal(UUID userId, BigDecimal amount, String reference);

    void creditPointsInternal(UUID userId, BigDecimal points, String reference);
}

