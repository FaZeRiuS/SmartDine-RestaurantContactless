package com.example.CourseWork.mapper;

import com.example.CourseWork.dto.CartItemDetailDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.Dish;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CartMapper {
    
    public CartResponseDto toResponseDto(Cart cart) {
        CartResponseDto dto = new CartResponseDto();
        dto.setCartId(cart.getId());
        dto.setUserId(cart.getUserId());

        var items = cart.getItems().stream().map(item -> {
            CartItemDetailDto detail = new CartItemDetailDto();
            detail.setId(item.getId());
            Dish dish = item.getDish();
            if (dish != null) {
                detail.setDishId(dish.getId());
                detail.setDishName(dish.getName());
                detail.setPrice(dish.getPrice());
            } else {
                detail.setDishId(null);
                detail.setDishName("(видалена страва)");
                detail.setPrice(java.math.BigDecimal.ZERO);
            }
            detail.setQuantity(item.getQuantity());
            detail.setSpecialRequest(item.getSpecialRequest());
            return detail;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
} 