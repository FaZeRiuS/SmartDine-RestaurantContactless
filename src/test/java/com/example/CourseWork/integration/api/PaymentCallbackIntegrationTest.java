package com.example.CourseWork.integration.api;

import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.loyalty.LoyaltyService;
import com.example.CourseWork.service.sse.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class PaymentCallbackIntegrationTest {

    private static final String PRIVATE_KEY = "test-private";

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;

    @MockitoBean LoyaltyService loyaltyService;
    @MockitoBean SseService sseService;

    @BeforeEach
    void resetDb() {
        orderRepository.deleteAll();
    }

    private record CallbackPayload(String data, String signature) {}

    private CallbackPayload buildCallback(String orderId, String status) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("order_id", orderId);
        payload.put("amount", "10.00");
        payload.put("currency", "UAH");

        String json = objectMapper.writeValueAsString(payload);
        String data = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        String signature = buildSignature(data);
        return new CallbackPayload(data, signature);
    }

    private String buildSignature(String data) throws Exception {
        String input = PRIVATE_KEY + data + PRIVATE_KEY;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha1.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void callback_success_setsPaymentSuccess_andCompletesReadyOrder_andPublishesUpdate() throws Exception {
        UUID userUuid = UUID.randomUUID();

        Order order = new Order();
        order.setUserId(userUuid.toString());
        order.setStatus(OrderStatus.READY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        Order saved = orderRepository.findById(java.util.Objects.requireNonNull(order.getId())).orElseThrow();
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        verify(sseService, times(1)).sendOrderUpdate(eq(userUuid.toString()), any());
        verify(loyaltyService, times(1)).earnPointsInternal(eq(userUuid), any(), eq("LIQPAY:order:" + order.getId()));
    }

    @Test
    void callback_isIdempotent_secondCallDoesNotDuplicateSideEffects() throws Exception {
        UUID userUuid = UUID.randomUUID();

        Order order = new Order();
        order.setUserId(userUuid.toString());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk());

        verify(loyaltyService, times(1)).earnPointsInternal(eq(userUuid), any(), eq("LIQPAY:order:" + order.getId()));
    }

    @Test
    void callback_ignoredStatus_doesNotMutateDb() throws Exception {
        Order order = new Order();
        order.setUserId("guest-user");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "error");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ignored status=error")));

        Order saved = orderRepository.findById(java.util.Objects.requireNonNull(order.getId())).orElseThrow();
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(sseService, never()).sendOrderUpdate(any(), any());
        verify(loyaltyService, never()).earnPointsInternal(any(), any(), any());
    }

    @Test
    void callback_sandbox_isTreatedAsSuccess() throws Exception {
        Order order = new Order();
        order.setUserId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "sandbox");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        Order saved = orderRepository.findById(java.util.Objects.requireNonNull(order.getId())).orElseThrow();
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void callback_invalidBase64_returns400() throws Exception {
        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", "not-base64!!!")
                        .param("signature", "sig"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_unknownOrder_returns404() throws Exception {
        CallbackPayload cb = buildCallback("order_999999_123456", "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isNotFound());
    }

    @Test
    void callback_missingOrderId_returns400() throws Exception {
        CallbackPayload cb = buildCallback("", "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing order_id"));
    }

    @Test
    void callback_badSignature_returns400() throws Exception {
        Order order = new Order();
        order.setUserId("guest-user");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature() + "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_nonUuidUser_doesNotEarnCashback() throws Exception {
        Order order = new Order();
        order.setUserId("GUEST_abc");
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("100.00"));
        order = orderRepository.save(order);

        String liqpayOrderId = "order_" + order.getId() + "_123456";
        CallbackPayload cb = buildCallback(liqpayOrderId, "success");

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", cb.data())
                        .param("signature", cb.signature()))
                .andExpect(status().isOk());

        verify(loyaltyService, never()).earnPointsInternal(any(), any(), any());
    }
}

