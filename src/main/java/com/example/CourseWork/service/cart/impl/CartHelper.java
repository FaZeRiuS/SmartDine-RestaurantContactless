package com.example.CourseWork.service.cart.impl;

import com.example.CourseWork.model.Cart;
import com.example.CourseWork.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class CartHelper {

    private final CartRepository cartRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Cart createCartRequiresNew(String userId) {
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setItems(new ArrayList<>());
        return cartRepository.saveAndFlush(newCart);
    }
}
