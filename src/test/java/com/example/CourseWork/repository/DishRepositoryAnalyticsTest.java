package com.example.CourseWork.repository;

import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class DishRepositoryAnalyticsTest {

    @Autowired private DishRepository dishRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private OrderRepository orderRepository;

    @Test
    void findRecommendedDishes_ShouldReturnDishesByPopularityAndTags() {
        // Arrange
        Menu mainMenu = new Menu();
        mainMenu.setName("Main Menu");
        menuRepository.save(mainMenu);

        createDish("Pizza", true, mainMenu);
        Dish d2 = createDish("Burger", true, mainMenu);
        
        // Simulate popularity for Burger (d2)
        createOrderWithDish(d2);
        createOrderWithDish(d2);
        
        // Act
        List<Dish> recommended = dishRepository.findRecommendedDishes("user-1");

        // Assert
        assertThat(recommended).isNotEmpty();
        // Since Burger is more popular, it should be high in score
        assertThat(recommended.get(0).getName()).isEqualTo("Burger");
    }

    @Test
    void checkIfSharesMenu_ShouldReturnTrueIfTogether() {
        // Arrange
        Menu menu1 = new Menu();
        menu1.setName("Breakfast");
        menuRepository.save(menu1);

        Dish d1 = createDish("Eggs", true, menu1);
        Dish d2 = createDish("Toast", true, menu1);
        Dish d3 = createDish("Steak", true, null);

        // Act & Assert
        assertThat(dishRepository.checkIfSharesMenu(d1.getId(), List.of(d2.getId()))).isTrue();
        assertThat(dishRepository.checkIfSharesMenu(d1.getId(), List.of(d3.getId()))).isFalse();
    }

    private Dish createDish(String name, boolean available, Menu menu) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setIsAvailable(available);
        dish.setPrice(BigDecimal.valueOf(10.0));
        if (menu != null) {
            dish.setMenus(List.of(menu));
        }
        return dishRepository.save(dish);
    }

    private void createOrderWithDish(Dish dish) {
        Order order = new Order();
        order.setUserId("test-user");
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setCreatedAt(OffsetDateTime.now());
        
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setDish(dish);
        item.setQuantity(1);
        order.setItems(List.of(item));
        
        orderRepository.save(order);
    }
}
