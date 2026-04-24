package com.example.CourseWork.service.order.component;

import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderTotalCalculator {

    private static final int MONEY_SCALE = 2;

    public BigDecimal money(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return items.stream()
                .map(item -> item.getDish().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public void recalculateAndSetTotal(Order order) {
        order.setTotalPrice(calculateTotal(order.getItems()));
    }

    public int calculateTotalPreparationTimeMinutes(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .filter(item -> item.getDish() != null && item.getDish().getPreparationTime() != null)
                .mapToInt(item -> item.getDish().getPreparationTime() * item.getQuantity())
                .sum();
    }
}

