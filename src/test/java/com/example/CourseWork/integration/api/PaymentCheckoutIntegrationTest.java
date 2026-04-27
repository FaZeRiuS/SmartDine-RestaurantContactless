package com.example.CourseWork.integration.api;

import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.security.CurrentUserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "liqpay.public.key=test-public",
        "liqpay.private.key=test-private",
        "app.public.base-url=http://localhost:8081"
})
@Import({
        com.example.CourseWork.config.SecurityConfig.class,
        com.example.CourseWork.config.SecurityProperties.class,
        com.example.CourseWork.config.OAuth2ClientTestStubConfig.class
})
@SuppressWarnings("null")
class PaymentCheckoutIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;

    @MockitoBean CurrentUserIdentity currentUserIdentity;

    @BeforeEach
    void resetDb() {
        orderRepository.deleteAll();
    }

    @org.springframework.lang.NonNull
    private RequestPostProcessor login(String userId, String role) {
        doReturn(userId).when(currentUserIdentity).currentUserId();
        return oidcLogin()
                .idToken(token -> token.subject(userId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void checkout_deniesNonOwner_with403() throws Exception {
        Order order = new Order();
        order.setUserId("owner-1");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        mockMvc.perform(get("/api/payment/checkout")
                        .with(login("other-user", "CUSTOMER"))
                        .param("orderId", order.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkout_deniesAlreadyPaid_with400() throws Exception {
        Order order = new Order();
        order.setUserId("cust-1");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        mockMvc.perform(get("/api/payment/checkout")
                        .with(login("cust-1", "CUSTOMER"))
                        .param("orderId", order.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_returnsFormParams_with200() throws Exception {
        Order order = new Order();
        order.setUserId("cust-2");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotalPrice(new BigDecimal("110.00"));
        order = orderRepository.save(order);

        mockMvc.perform(get("/api/payment/checkout")
                        .with(login("cust-2", "CUSTOMER"))
                        .param("orderId", order.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionUrl").isNotEmpty())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.signature").isNotEmpty())
                .andExpect(jsonPath("$.liqpayOrderId").isNotEmpty());
    }
}

