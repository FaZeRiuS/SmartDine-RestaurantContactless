package com.example.CourseWork.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryDto(
        BigDecimal revenueToday,
        BigDecimal revenueLast7Days,
        long successfulOrdersToday,
        BigDecimal averageCheckLast7Days
) {}

