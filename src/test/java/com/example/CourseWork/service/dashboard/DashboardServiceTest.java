package com.example.CourseWork.service.dashboard;

import com.example.CourseWork.dto.dashboard.DashboardViewDto;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderRepository.HourCountView;
import com.example.CourseWork.repository.OrderRepository.TopDishView;
import com.example.CourseWork.service.dashboard.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Clock appClock;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private void mockClockKyivFixed() {
        when(appClock.getZone()).thenReturn(ZoneId.of("Europe/Kyiv"));
        when(appClock.instant()).thenReturn(Instant.parse("2026-04-15T10:00:00Z"));
    }

    @Test
    void getAdminDashboard_ShouldCalculateSummaryCorrectly() {
        // Arrange
        mockClockKyivFixed();
        when(orderRepository.sumRevenue(any(), any())).thenReturn(BigDecimal.valueOf(500.0));
        when(orderRepository.countSuccessfulOrders(any(), any())).thenReturn(10L);
        when(orderRepository.avgCheck(any(), any())).thenReturn(BigDecimal.valueOf(50.0));
        when(orderRepository.findTopDishes(any(), any())).thenReturn(Collections.emptyList());
        when(orderRepository.countSuccessfulOrdersByHour(any(), any())).thenReturn(Collections.emptyList());

        // Act
        DashboardViewDto result = dashboardService.getAdminDashboard();

        // Assert
        assertThat(result.summary().revenueToday()).isEqualByComparingTo("500.0");
        assertThat(result.summary().successfulOrdersToday()).isEqualTo(10L);
        assertThat(result.summary().averageCheckLast7Days()).isEqualByComparingTo("50.0");
    }

    @Test
    void getAdminDashboard_ShouldHandleEmptyDataWithSafes() {
        // Arrange
        mockClockKyivFixed();
        when(orderRepository.sumRevenue(any(), any())).thenReturn(null);
        when(orderRepository.countSuccessfulOrders(any(), any())).thenReturn(null);
        when(orderRepository.avgCheck(any(), any())).thenReturn(null);
        when(orderRepository.findTopDishes(any(), any())).thenReturn(Collections.emptyList());
        when(orderRepository.countSuccessfulOrdersByHour(any(), any())).thenReturn(Collections.emptyList());

        // Act
        DashboardViewDto result = dashboardService.getAdminDashboard();

        // Assert
        assertThat(result.summary().revenueToday()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.summary().successfulOrdersToday()).isEqualTo(0L);
        assertThat(result.summary().averageCheckLast7Days()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAdminDashboard_ShouldFill24HoursInHourlyStats() {
        // Arrange
        mockClockKyivFixed();
        HourCountView hour10 = mock(HourCountView.class);
        when(hour10.getOrderHour()).thenReturn(10);
        when(hour10.getCount()).thenReturn(5L);

        when(orderRepository.countSuccessfulOrdersByHour(any(), any())).thenReturn(List.of(hour10));
        when(orderRepository.findTopDishes(any(), any())).thenReturn(Collections.emptyList());

        // Act
        DashboardViewDto result = dashboardService.getAdminDashboard();

        // Assert
        assertThat(result.hourlyOrdersToday()).hasSize(24);
        assertThat(result.hourlyOrdersToday().get(10).count()).isEqualTo(5L);
        assertThat(result.hourlyOrdersToday().get(9).count()).isEqualTo(0L);
        assertThat(result.hourlyOrdersToday().get(11).count()).isEqualTo(0L);
    }

    @Test
    void getAdminDashboard_ShouldMapTopDishesCorrectly() {
        // Arrange
        mockClockKyivFixed();
        TopDishView dish1 = mock(TopDishView.class);
        when(dish1.getName()).thenReturn("Pizza");
        when(dish1.getQuantity()).thenReturn(20L);

        when(orderRepository.findTopDishes(any(), any())).thenReturn(List.of(dish1));
        when(orderRepository.countSuccessfulOrdersByHour(any(), any())).thenReturn(Collections.emptyList());

        // Act
        DashboardViewDto result = dashboardService.getAdminDashboard();

        // Assert
        assertThat(result.topDishes()).hasSize(1);
        assertThat(result.topDishes().get(0).name()).isEqualTo("Pizza");
        assertThat(result.topDishes().get(0).quantity()).isEqualTo(20L);
    }
}
