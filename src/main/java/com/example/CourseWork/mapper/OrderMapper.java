package com.example.CourseWork.mapper;

import com.example.CourseWork.dto.order.OrderItemResponseDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final Clock appClock;

    public OrderMapper(Clock appClock) {
        this.appClock = appClock;
    }
    
    public OrderResponseDto toResponseDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setCreatedAt(
                order.getCreatedAt() == null
                        ? null
                        : order.getCreatedAt().atZoneSameInstant(appClock.getZone()).toOffsetDateTime()
        );
        dto.setTotalPrice(order.getTotalPrice());
        BigDecimal total = normalize(order.getTotalPrice());
        BigDecimal discount = (order.getLoyaltyDiscount() == null ? BigDecimal.ZERO : order.getLoyaltyDiscount())
                .setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(total) > 0) discount = total;
        BigDecimal tip = (order.getTipAmount() == null ? BigDecimal.ZERO : order.getTipAmount())
                .setScale(2, RoundingMode.HALF_UP);
        if (tip.compareTo(BigDecimal.ZERO) < 0) tip = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal payable = total.subtract(discount).add(tip).setScale(2, RoundingMode.HALF_UP);
        dto.setLoyaltyDiscount(discount);
        dto.setAmountToPay(payable);
        dto.setTipAmount(tip);
        dto.setTableNumber(order.getTableNumber());
        dto.setNeedsWaiter(order.isNeedsWaiter());
        dto.setEstimatedReadyTime(order.getEstimatedReadyTime());

        List<OrderItemResponseDto> items = (order.getItems() == null ? Collections.<OrderItem>emptyList() : order.getItems())
                .stream().map(item -> {
            OrderItemResponseDto itemDto = new OrderItemResponseDto();
            itemDto.setId(item.getId());
            Dish dish = item.getDish();
            if (dish != null) {
                itemDto.setDishId(dish.getId());
                itemDto.setDishName(dish.getName());
                itemDto.setPrice(dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice());
            } else {
                itemDto.setDishId(null);
                itemDto.setDishName("(видалена страва)");
                itemDto.setPrice(BigDecimal.ZERO);
            }
            itemDto.setQuantity(item.getQuantity());
            itemDto.setSpecialRequest(item.getSpecialRequest());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(2, RoundingMode.HALF_UP);
    }
} 