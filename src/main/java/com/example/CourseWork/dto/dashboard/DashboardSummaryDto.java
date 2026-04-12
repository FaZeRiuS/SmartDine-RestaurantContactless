package com.example.CourseWork.dto.dashboard;

public record DashboardSummaryDto(
        double revenueToday,
        double revenueLast7Days,
        long successfulOrdersToday,
        double averageCheckLast7Days
) {}

