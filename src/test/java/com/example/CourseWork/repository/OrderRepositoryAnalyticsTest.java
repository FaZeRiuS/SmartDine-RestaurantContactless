package com.example.CourseWork.repository;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryAnalyticsTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void sumRevenue_ShouldCalculateCorrectly() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        createOrder(100.0, PaymentStatus.SUCCESS, now.minusMinutes(10));
        createOrder(50.0, PaymentStatus.SUCCESS, now.minusMinutes(5));
        createOrder(200.0, PaymentStatus.PENDING, now.minusMinutes(2)); // Should be ignored

        // Act
        Double revenue = orderRepository.sumRevenue(now.minusHours(1), now.plusHours(1));

        // Assert
        assertThat(revenue).isEqualTo(150.0);
    }

    @Test
    void avgCheck_ShouldCalculateCorrectly() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        createOrder(100.0, PaymentStatus.SUCCESS, now.minusMinutes(10));
        createOrder(200.0, PaymentStatus.SUCCESS, now.minusMinutes(5));

        // Act
        Double avg = orderRepository.avgCheck(now.minusHours(1), now.plusHours(1));

        // Assert
        assertThat(avg).isEqualTo(150.0);
    }

    @Test
    void countSuccessfulOrdersByHour_ShouldGroupCorrectly() {
        // Arrange
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0);
        createOrder(50.0, PaymentStatus.SUCCESS, today);         // Hour 10
        createOrder(60.0, PaymentStatus.SUCCESS, today.plusHours(1)); // Hour 11
        createOrder(70.0, PaymentStatus.SUCCESS, today.plusHours(1)); // Hour 11 (total 2)

        // Act
        List<OrderRepository.HourCountView> stats = orderRepository.countSuccessfulOrdersByHour(
                today.minusHours(1), today.plusHours(5));

        // Assert
        assertThat(stats).isNotEmpty();
        
        OrderRepository.HourCountView h10 = stats.stream().filter(s -> s.getOrderHour() == 10).findFirst().orElseThrow();
        OrderRepository.HourCountView h11 = stats.stream().filter(s -> s.getOrderHour() == 11).findFirst().orElseThrow();
        
        assertThat(h10.getCount()).isEqualTo(1L);
        assertThat(h11.getCount()).isEqualTo(2L);
    }

    private void createOrder(double total, PaymentStatus payStatus, LocalDateTime createdAt) {
        Order order = new Order();
        order.setTotalPrice((float) total);
        order.setPaymentStatus(payStatus);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(createdAt);
        order.setUserId("test-user");
        orderRepository.save(order);
    }
}
