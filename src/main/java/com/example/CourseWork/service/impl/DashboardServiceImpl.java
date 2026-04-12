package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.dashboard.DashboardSummaryDto;
import com.example.CourseWork.dto.dashboard.DashboardViewDto;
import com.example.CourseWork.dto.dashboard.HourlyOrdersDto;
import com.example.CourseWork.dto.dashboard.TopDishDto;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;

    @Override
    public DashboardViewDto getAdminDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        LocalDateTime startOf7Days = today.minusDays(6).atStartOfDay(); // inclusive window: today + previous 6 days

        double revenueToday = safeDouble(orderRepository.sumRevenue(startOfToday, startOfTomorrow));
        double revenueLast7 = safeDouble(orderRepository.sumRevenue(startOf7Days, startOfTomorrow));

        long successfulOrdersToday = safeLong(orderRepository.countSuccessfulOrders(startOfToday, startOfTomorrow));
        double avgCheckLast7 = safeDouble(orderRepository.avgCheck(startOf7Days, startOfTomorrow));

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

    private static double safeDouble(Double v) {
        return v == null ? 0.0 : v;
    }

    private static long safeLong(Long v) {
        return v == null ? 0L : v;
    }
}

