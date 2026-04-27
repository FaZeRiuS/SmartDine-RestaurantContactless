package com.example.CourseWork.integration.repo;

import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("tc")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class OrderRepositoryAnalyticsPostgresIT extends AbstractPostgresTcTest {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void sumRevenue_worksOnRealPostgres() {
        OffsetDateTime now = OffsetDateTime.now();
        persistOrderRow(new BigDecimal("100.00"), PaymentStatus.SUCCESS, now.minusMinutes(10));
        persistOrderRow(new BigDecimal("50.00"), PaymentStatus.SUCCESS, now.minusMinutes(5));
        persistOrderRow(new BigDecimal("200.00"), PaymentStatus.PENDING, now.minusMinutes(2));

        BigDecimal revenue = orderRepository.sumRevenue(now.minusHours(1), now.plusHours(1));
        assertThat(revenue).isEqualByComparingTo("150.00");
    }

    @Test
    void avgCheck_worksOnRealPostgres() {
        OffsetDateTime now = OffsetDateTime.now();
        persistOrderRow(new BigDecimal("100.00"), PaymentStatus.SUCCESS, now.minusMinutes(10));
        persistOrderRow(new BigDecimal("200.00"), PaymentStatus.SUCCESS, now.minusMinutes(5));

        BigDecimal avg = orderRepository.avgCheck(now.minusHours(1), now.plusHours(1));
        assertThat(avg).isEqualByComparingTo("150.00");
    }

    @Test
    void countSuccessfulOrdersByHour_worksOnRealPostgres() {
        OffsetDateTime today10 = OffsetDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        persistOrderRow(new BigDecimal("50.00"), PaymentStatus.SUCCESS, today10);
        persistOrderRow(new BigDecimal("60.00"), PaymentStatus.SUCCESS, today10.plusHours(1));
        persistOrderRow(new BigDecimal("70.00"), PaymentStatus.SUCCESS, today10.plusHours(1));

        var stats = orderRepository.countSuccessfulOrdersByHour(today10.minusHours(1), today10.plusHours(5));
        assertThat(stats).isNotEmpty();

        OrderRepository.HourCountView h10 = stats.stream().filter(s -> s.getOrderHour() == 10).findFirst()
                .orElseThrow();
        OrderRepository.HourCountView h11 = stats.stream().filter(s -> s.getOrderHour() == 11).findFirst()
                .orElseThrow();

        assertThat(h10.getCount()).isEqualTo(1L);
        assertThat(h11.getCount()).isEqualTo(2L);
    }

    @Test
    void findTopDishes_worksOnRealPostgres() {
        OffsetDateTime now = OffsetDateTime.now();

        Integer dishA = persistDish("A");
        Integer dishB = persistDish("B");

        Order o1 = persistOrder(new BigDecimal("20.00"), PaymentStatus.SUCCESS, now.minusMinutes(10));
        persistOrderItem(o1.getId(), dishA, 3);
        persistOrderItem(o1.getId(), dishB, 1);

        Order o2 = persistOrder(new BigDecimal("10.00"), PaymentStatus.SUCCESS, now.minusMinutes(5));
        persistOrderItem(o2.getId(), dishB, 4);

        var top = orderRepository.findTopDishes(now.minusHours(1), now.plusHours(1));
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).getName()).isEqualTo("B");
        assertThat(top.get(0).getQuantity()).isEqualTo(5L);
    }

    private void persistOrderRow(BigDecimal total, PaymentStatus paymentStatus, OffsetDateTime createdAt) {
        Order order = new Order();
        order.setUserId("pg-it-user");
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(paymentStatus);
        order.setCreatedAt(createdAt);
        order.setTotalPrice(total);
        entityManager.persist(order);
        entityManager.flush();
    }

    private Integer persistDish(String name) {
        com.example.CourseWork.model.Dish dish = new com.example.CourseWork.model.Dish();
        dish.setName(name);
        dish.setPrice(new BigDecimal("1.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(1);
        entityManager.persist(dish);
        entityManager.flush();
        return dish.getId();
    }

    private Order persistOrder(BigDecimal total, PaymentStatus paymentStatus, OffsetDateTime createdAt) {
        Order order = new Order();
        order.setUserId("pg-it-user");
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(paymentStatus);
        order.setCreatedAt(createdAt);
        order.setTotalPrice(total);
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }

    private void persistOrderItem(Integer orderId, Integer dishId, int quantity) {
        com.example.CourseWork.model.OrderItem item = new com.example.CourseWork.model.OrderItem();
        item.setOrder(entityManager.getEntityManager().getReference(Order.class, orderId));
        item.setDish(entityManager.getEntityManager().getReference(com.example.CourseWork.model.Dish.class, dishId));
        item.setQuantity(quantity);
        entityManager.persist(item);
        entityManager.flush();
    }
}
