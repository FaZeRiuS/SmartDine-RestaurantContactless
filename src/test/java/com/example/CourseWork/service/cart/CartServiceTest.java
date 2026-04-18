package com.example.CourseWork.service.cart;

import com.example.CourseWork.dto.cart.CartItemDto;
import com.example.CourseWork.dto.cart.CartResponseDto;
import com.example.CourseWork.mapper.CartMapper;
import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.CartItem;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.CartRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.cart.impl.CartServiceImpl;
import com.example.CourseWork.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private DishRepository dishRepository;
    @Mock private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final String USER_ID = "user-123";

    @Test
    void getCartByUserId_WhenCartDoesNotExist_ShouldCreateNew() {
        // Arrange
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArguments()[0]);
        when(cartMapper.toResponseDto(any(Cart.class))).thenReturn(new CartResponseDto());

        // Act
        CartResponseDto result = cartService.getCartByUserId(USER_ID);

        // Assert
        assertThat(result).isNotNull();
        verify(cartRepository).save(argThat(cart -> cart.getUserId().equals(USER_ID)));
    }

    @Test
    void addItemToCart_WhenNewItem_ShouldAddToList() {
        // Arrange
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        cart.setItems(new ArrayList<>());

        Dish dish = new Dish();
        dish.setId(10);
        dish.setIsAvailable(true);

        CartItemDto dto = new CartItemDto();
        dto.setDishId(10);
        dto.setQuantity(2);
        dto.setSpecialRequest("No spicy");

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(dishRepository.findById(10)).thenReturn(Optional.of(dish));
        when(cartMapper.toResponseDto(any(Cart.class))).thenReturn(new CartResponseDto());

        // Act
        cartService.addItemToCart(USER_ID, dto);

        // Assert
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_WhenExistingItemWithSameRequest_ShouldIncrementQuantity() {
        // Arrange
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        cart.setItems(new ArrayList<>());

        Dish dish = new Dish();
        dish.setId(10);
        dish.setIsAvailable(true);

        CartItem existingItem = new CartItem();
        existingItem.setDish(dish);
        existingItem.setQuantity(1);
        existingItem.setSpecialRequest("Extra salt");
        cart.getItems().add(existingItem);

        CartItemDto dto = new CartItemDto();
        dto.setDishId(10);
        dto.setQuantity(3);
        dto.setSpecialRequest("Extra salt");

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(dishRepository.findById(10)).thenReturn(Optional.of(dish));

        // Act
        cartService.addItemToCart(USER_ID, dto);

        // Assert
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
    }

    @Test
    void addItemToCart_WhenDishNotAvailable_ShouldThrowException() {
        // Arrange
        Cart cart = new Cart();
        Dish dish = new Dish();
        dish.setId(20);
        dish.setIsAvailable(false); // UNAVAILABLE

        CartItemDto dto = new CartItemDto();
        dto.setDishId(20);
        dto.setQuantity(1);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(dishRepository.findById(20)).thenReturn(Optional.of(dish));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(USER_ID, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(com.example.CourseWork.exception.ErrorMessages.DISH_NOT_AVAILABLE);
    }

    @Test
    void updateCartItemQuantity_WhenQuantityZero_ShouldRemoveItem() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        
        CartItem item = new CartItem();
        item.setId(500);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        // Act
        cartService.updateCartItemQuantity(USER_ID, 500, 0);

        // Assert
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }
}
