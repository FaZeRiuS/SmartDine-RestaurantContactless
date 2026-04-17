package com.example.CourseWork.repository;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class OrderRepositoryCustomerOrdersFetchTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findPageWithItemsAndDishes_populatesCollectionsForDetachedUse() {
        Dish dish = new Dish();
        dish.setName("Soup");
        dish.setPrice(new BigDecimal("12.50"));
        dish.setIsAvailable(true);
        entityManager.persist(dish);

        Order order = new Order();
        order.setUserId("cust-fetch-test");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("12.50"));

        OrderItem line = new OrderItem();
        line.setOrder(order);
        line.setDish(dish);
        line.setQuantity(2);
        order.getItems().add(line);

        entityManager.persist(order);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findPageWithItemsAndDishesForUserAndStatuses(
                "cust-fetch-test", List.of(OrderStatus.NEW), pageable);

        assertThat(page.getContent()).hasSize(1);
        Order loaded = page.getContent().get(0);
        entityManager.clear();

        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(loaded.getItems().get(0).getDish().getName()).isEqualTo("Soup");
    }
}
