package com.example.CourseWork.service.cart.impl;

import com.example.CourseWork.dto.cart.CartItemDto;
import com.example.CourseWork.dto.cart.CartResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.mapper.CartMapper;
import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.CartItem;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.repository.CartRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.cart.CartService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final DishRepository dishRepository;
    private final CartMapper cartMapper;

    private static String normalizeSpecialRequest(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.isBlank() ? "" : v;
    }

    @Transactional
    @Override
    public CartResponseDto getCartByUserId(String userId) {
        return cartRepository.findByUserIdWithItemsAndDishes(userId)
                .map(cartMapper::toResponseDto)
                .orElseGet(() -> {
                    try {
                        Cart newCart = new Cart();
                        newCart.setUserId(userId);
                        newCart.setItems(new ArrayList<>());
                        Cart saved = cartRepository.save(newCart);
                        return cartMapper.toResponseDto(saved);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return cartRepository.findByUserIdWithItemsAndDishes(userId)
                                .map(cartMapper::toResponseDto)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Transactional
    @Override
    public CartResponseDto addItemToCart(String userId, CartItemDto itemDto) {
        if (itemDto == null || itemDto.getDishId() == null) {
            throw new BadRequestException(ErrorMessages.BAD_REQUEST);
        }
        if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
            throw new BadRequestException(ErrorMessages.INVALID_QUANTITY);
        }

        Integer dishId = itemDto.getDishId();

        Cart cart = cartRepository.findByUserIdWithItemsAndDishes(userId).orElseGet(() -> {
            try {
                Cart newCart = new Cart();
                newCart.setUserId(userId);
                newCart.setItems(new ArrayList<>());
                return cartRepository.save(newCart);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                return cartRepository.findByUserIdWithItemsAndDishes(userId)
                        .orElseThrow(() -> e);
            }
        });

        @SuppressWarnings("null")
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));

        if (Boolean.FALSE.equals(dish.getIsAvailable())) {
            throw new BadRequestException(ErrorMessages.DISH_NOT_AVAILABLE);
        }
        final String req = normalizeSpecialRequest(itemDto.getSpecialRequest());
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getDish().getId().equals(dish.getId()) &&
                        Objects.equals(normalizeSpecialRequest(item.getSpecialRequest()), req))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + itemDto.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setDish(dish);
            item.setQuantity(itemDto.getQuantity());
            item.setSpecialRequest(req);
            cart.getItems().add(item);
        }

        cartRepository.save(cart);

        return cartMapper.toResponseDto(cart);
    }

    @Transactional
    @Override
    public CartResponseDto updateCartItemQuantity(String userId, Integer itemId, Integer quantity) {
        Cart cart = cartRepository.findByUserIdWithItemsAndDishes(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_NOT_FOUND));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_ITEM_NOT_FOUND));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        cartRepository.save(cart);
        return cartMapper.toResponseDto(cart);
    }

    @Transactional
    @Override
    public CartResponseDto updateCartItemSpecialRequest(String userId, Integer itemId, String specialRequest) {
        Cart cart = cartRepository.findByUserIdWithItemsAndDishes(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_NOT_FOUND));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_ITEM_NOT_FOUND));

        item.setSpecialRequest(normalizeSpecialRequest(specialRequest));
        cartRepository.save(cart);
        return cartMapper.toResponseDto(cart);
    }

    @Transactional
    @Override
    public CartResponseDto removeCartItem(String userId, Integer itemId) {
        Cart cart = cartRepository.findByUserIdWithItemsAndDishes(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_NOT_FOUND));

        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepository.save(cart);
        
        return cartMapper.toResponseDto(cart);
    }
}
