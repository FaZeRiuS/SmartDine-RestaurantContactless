package com.example.CourseWork.dto.dashboard;

import java.util.List;

public record DashboardViewDto(
        DashboardSummaryDto summary,
        List<TopDishDto> topDishes,
        List<HourlyOrdersDto> hourlyOrdersToday
) {}

