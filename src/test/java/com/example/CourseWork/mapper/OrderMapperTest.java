package com.example.CourseWork.mapper;

import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper(Clock.system(ZoneId.of("Europe/Kyiv")));

    @Test
    void toResponseDto_ShouldMapBasicFields() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId("user123");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(BigDecimal.valueOf(100.0));
        order.setTableNumber(5);

        // Act
        OrderResponseDto result = orderMapper.toResponseDto(order);

        // Assert
        assertThat(result.getId()).isEqualTo(order.getId());
        assertThat(result.getUserId()).isEqualTo(order.getUserId());
        assertThat(result.getStatus()).isEqualTo(order.getStatus());
        assertThat(result.getPaymentStatus()).isEqualTo(order.getPaymentStatus());
        assertThat(result.getTotalPrice()).isEqualByComparingTo(order.getTotalPrice());
        assertThat(result.getTableNumber()).isEqualTo(order.getTableNumber());
    }

    @Test
    void toResponseDto_ShouldCalculateAmountsCorrectly() {
        // Arrange
        Order order = new Order();
        order.setTotalPrice(BigDecimal.valueOf(100.0));
        order.setLoyaltyDiscount(new BigDecimal("15.50"));
        order.setTipAmount(new BigDecimal("10.00"));

        // Payable = 100.0 - 15.5 + 10.0 = 94.50

        // Act
        OrderResponseDto result = orderMapper.toResponseDto(order);

        // Assert
        assertThat(result.getLoyaltyDiscount()).isEqualByComparingTo("15.5");
        assertThat(result.getTipAmount()).isEqualByComparingTo("10.0");
        assertThat(result.getAmountToPay()).isEqualByComparingTo("94.5");
    }

    @Test
    void toResponseDto_ShouldCapDiscountAtTotalPrice() {
        // Arrange
        Order order = new Order();
        order.setTotalPrice(BigDecimal.valueOf(50.0));
        order.setLoyaltyDiscount(new BigDecimal("60.00")); // Discount > Total
        order.setTipAmount(BigDecimal.ZERO);

        // Act
        OrderResponseDto result = orderMapper.toResponseDto(order);

        // Assert
        assertThat(result.getLoyaltyDiscount()).isEqualByComparingTo("50.0");
        assertThat(result.getAmountToPay()).isEqualByComparingTo("0.0");
    }

    @Test
    void toResponseDto_ShouldHandleNegativeAmounts_AsZero() {
        // Arrange
        Order order = new Order();
        order.setTotalPrice(BigDecimal.valueOf(100.0));
        order.setLoyaltyDiscount(new BigDecimal("-10.00")); // Should be treated as 0
        order.setTipAmount(new BigDecimal("-5.00"));        // Should be treated as 0

        // Act
        OrderResponseDto result = orderMapper.toResponseDto(order);

        // Assert
        assertThat(result.getLoyaltyDiscount()).isEqualByComparingTo("0.0");
        assertThat(result.getTipAmount()).isEqualByComparingTo("0.0");
        assertThat(result.getAmountToPay()).isEqualByComparingTo("100.0");
    }

    @Test
    void toResponseDto_ShouldMapOrderItems() {
        // Arrange
        Order order = new Order();
        order.setItems(new ArrayList<>());

        Dish dish = new Dish();
        dish.setId(10);
        dish.setName("Pizza");
        dish.setPrice(BigDecimal.valueOf(12.5));

        OrderItem item = new OrderItem();
        item.setId(1);
        item.setDish(dish);
        item.setQuantity(2);
        item.setSpecialRequest("No onions");
        order.getItems().add(item);

        // Act
        OrderResponseDto result = orderMapper.toResponseDto(order);

        // Assert
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getDishName()).isEqualTo("Pizza");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(0).getPrice()).isEqualByComparingTo("12.5");
    }
}
