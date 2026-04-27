package com.example.CourseWork.integration.api;

import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.CartItem;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class OrderFlowIntegrationTest {

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
    void createOrder_persistsOrderAndItems_andReturns200() throws Exception {
        Dish dish = new Dish();
        dish.setName("Pizza");
        dish.setPrice(new BigDecimal("120.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(15);
        dish = dishRepository.save(dish);

        mockMvc.perform(post("/api/orders")
                        .with(login("user-1", "CUSTOMER"))
                        .with(csrf())
                        .sessionAttr("tableNumber", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"dishId\":" + dish.getId() + ",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        assertThat(orderRepository.count()).isEqualTo(1);
        Order saved = orderRepository.findAll().get(0);
        saved = orderRepository.findByIdWithItemsAndDishes(saved.getId()).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getTableNumber()).isEqualTo(7);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(saved.getItems().get(0).getDish().getId()).isEqualTo(dish.getId());
    }

    @Test
    void confirmOrderFromCart_createsOrder_andClearsCart() throws Exception {
        Dish dish = new Dish();
        dish.setName("Soup");
        dish.setPrice(new BigDecimal("50.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(5);
        dish = dishRepository.save(dish);

        Cart cart = new Cart();
        cart.setUserId("user-2");
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setDish(dish);
        item.setQuantity(3);
        cart.getItems().add(item);
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders/confirm")
                        .with(login("user-2", "CUSTOMER"))
                        .with(csrf())
                        .sessionAttr("tableNumber", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        assertThat(orderRepository.count()).isEqualTo(1);
        Cart loadedCart = cartRepository.findByUserIdWithItemsAndDishes("user-2").orElseThrow();
        assertThat(loadedCart.getItems()).isEmpty();
    }

    @Test
    void myActiveOrder_setsNoStoreCacheControl() throws Exception {
        Dish dish = new Dish();
        dish.setName("Tea");
        dish.setPrice(new BigDecimal("10.00"));
        dish.setIsAvailable(true);
        dish.setPreparationTime(1);
        dish = dishRepository.save(dish);

        Order order = new Order();
        order.setUserId("user-3");
        order.setTableNumber(1);
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setItems(new ArrayList<>());

        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setDish(dish);
        oi.setQuantity(1);
        order.getItems().add(oi);
        order.setTotalPrice(new BigDecimal("10.00"));
        orderRepository.save(order);

        mockMvc.perform(get("/api/orders/my-active")
                        .with(login("user-3", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.CoreMatchers.containsString("no-store")));
    }
}

