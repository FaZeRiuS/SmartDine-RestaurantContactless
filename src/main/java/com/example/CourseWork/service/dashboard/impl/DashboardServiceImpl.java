package com.example.CourseWork.service.dashboard.impl;

import com.example.CourseWork.dto.dashboard.DashboardSummaryDto;
import com.example.CourseWork.dto.dashboard.DashboardViewDto;
import com.example.CourseWork.dto.dashboard.HourlyOrdersDto;
import com.example.CourseWork.dto.dashboard.TopDishDto;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final Clock appClock;

    @Override
    public DashboardViewDto getAdminDashboard() {
        ZoneId zone = appClock.getZone();
        LocalDate today = LocalDate.now(appClock);
        OffsetDateTime startOfToday = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        OffsetDateTime startOf7Days = today.minusDays(6).atStartOfDay(zone).toOffsetDateTime(); // inclusive window: today + previous 6 days

        BigDecimal revenueToday = safeMoney(orderRepository.sumRevenue(startOfToday, startOfTomorrow));
        BigDecimal revenueLast7 = safeMoney(orderRepository.sumRevenue(startOf7Days, startOfTomorrow));

        long successfulOrdersToday = safeLong(orderRepository.countSuccessfulOrders(startOfToday, startOfTomorrow));
        BigDecimal avgCheckLast7 = safeMoney(orderRepository.avgCheck(startOf7Days, startOfTomorrow));

        DashboardSummaryDto summary = new DashboardSummaryDto(
                revenueToday,
                revenueLast7,
                successfulOrdersToday,
                avgCheckLast7
        );

        List<TopDishDto> topDishes = orderRepository.findTopDishes(startOf7Days, startOfTomorrow).stream()
                .map(v -> new TopDishDto(v.getName(), safeLong(v.getQuantity())))
                .toList();

        // Hourly successful orders for today: build a 24-point series
        Map<Integer, Long> hourToCount = orderRepository.countSuccessfulOrdersByHour(startOfToday, startOfTomorrow).stream()
                .collect(Collectors.toMap(
                        v -> v.getOrderHour() == null ? 0 : v.getOrderHour(),
                        v -> safeLong(v.getCount())
                ));

        List<HourlyOrdersDto> hourly = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            hourly.add(new HourlyOrdersDto(h, hourToCount.getOrDefault(h, 0L)));
        }

        return new DashboardViewDto(summary, topDishes, hourly);
    }

    private static BigDecimal safeMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static long safeLong(Long v) {
        return v == null ? 0L : v;
    }
}

