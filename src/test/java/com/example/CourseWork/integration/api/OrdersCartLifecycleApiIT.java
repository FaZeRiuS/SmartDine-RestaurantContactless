package com.example.CourseWork.integration.api;

import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.repository.CartRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.security.CurrentUserIdentity;
import com.example.CourseWork.service.order.component.OrderNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        com.example.CourseWork.config.SecurityConfig.class,
        com.example.CourseWork.config.SecurityProperties.class,
        com.example.CourseWork.config.OAuth2ClientTestStubConfig.class
})
@SuppressWarnings("null")
class OrdersCartLifecycleApiIT {

    @Autowired MockMvc mockMvc;
    @Autowired DishRepository dishRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired CartRepository cartRepository;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;

    @MockitoBean CurrentUserIdentity currentUserIdentity;
    @MockitoBean OrderNotifier orderNotifier;

    @BeforeEach
    void resetDb() {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        dishRepository.deleteAll();
    }

    @org.springframework.lang.NonNull
    private RequestPostProcessor login(String userId, String role) {
        doReturn(userId).when(currentUserIdentity).currentUserId();
        return oidcLogin()
                .idToken(token -> token.subject(userId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void confirmOrderFromCart_cartNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/confirm")
                        .with(login("no-cart-user", "CUSTOMER"))
                        .with(csrf())
                        .sessionAttr("tableNumber", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmOrderFromCart_cartEmpty_returns400() throws Exception {
        Cart cart = new Cart();
        cart.setUserId("empty-cart-user");
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders/confirm")
                        .with(login("empty-cart-user", "CUSTOMER"))
                        .with(csrf())
                        .sessionAttr("tableNumber", 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myActiveOrder_whenNoActive_returns204_andNoStore() throws Exception {
        mockMvc.perform(get("/api/orders/my-active")
                        .with(login("user-x", "CUSTOMER")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void addItemsToOrder_readyTransitionsToPreparing() throws Exception {
        Dish dish = new Dish();
        dish.setName("Burger");
        dish.setPrice(new BigDecimal("70.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(10);
        dish = dishRepository.save(dish);

        Order order = new Order();
        order.setUserId("cust-1");
        order.setStatus(OrderStatus.READY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(new BigDecimal("70.00"));
        order.setItems(new ArrayList<>());
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setDish(dish);
        oi.setQuantity(1);
        order.getItems().add(oi);
        order = orderRepository.save(order);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/items")
                        .with(login("cust-1", "CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"dishId\":" + dish.getId() + ",\"quantity\":1}]}"))
                .andExpect(status().isOk());

        Order saved = orderRepository.findByIdWithItemsAndDishes(order.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void updateOrderItemQuantity_decreaseNotAllowedForNonNew_returns400() throws Exception {
        Dish dish = new Dish();
        dish.setName("Pasta");
        dish.setPrice(new BigDecimal("90.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(12);
        dish = dishRepository.save(dish);

        Order order = new Order();
        order.setUserId("cust-2");
        order.setStatus(OrderStatus.PREPARING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(new BigDecimal("180.00"));
        order.setItems(new ArrayList<>());
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setDish(dish);
        oi.setQuantity(2);
        order.getItems().add(oi);
        order = orderRepository.save(order);

        Order loaded = orderRepository.findByIdWithItemsAndDishes(order.getId()).orElseThrow();
        Integer itemId = loaded.getItems().get(0).getId();

        mockMvc.perform(put("/api/orders/" + order.getId() + "/items/" + itemId + "/quantity")
                        .with(login("cust-2", "CUSTOMER"))
                        .with(csrf())
                        .param("quantity", "1"))
                .andExpect(status().isBadRequest());
    }
}

