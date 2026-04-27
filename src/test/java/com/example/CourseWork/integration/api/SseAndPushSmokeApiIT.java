package com.example.CourseWork.integration.api;

import com.example.CourseWork.dto.push.PushSubscriptionDto;
import com.example.CourseWork.repository.PushSubscriptionRepository;
import com.example.CourseWork.security.CurrentUserIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class SseAndPushSmokeApiIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PushSubscriptionRepository pushSubscriptionRepository;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;
    @MockitoBean CurrentUserIdentity currentUserIdentity;

    @BeforeEach
    void resetDb() {
        pushSubscriptionRepository.deleteAll();
    }

    @org.springframework.lang.NonNull
    private RequestPostProcessor login(String userId, String role) {
        doReturn(userId).when(currentUserIdentity).currentUserId();
        return oidcLogin()
                .idToken(token -> token.subject(userId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void sseSubscribe_returnsEventStream() throws Exception {
        mockMvc.perform(get("/api/sse/subscribe/user-1")
                        .with(login("user-1", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void pushSubscribe_persistsSubscription() throws Exception {
        PushSubscriptionDto dto = new PushSubscriptionDto();
        dto.setEndpoint("https://example.test/ep");
        dto.setP256dh("p256dh");
        dto.setAuth("auth");

        mockMvc.perform(post("/api/notifications/subscribe")
                        .with(login("cust-1", "CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(java.util.Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isOk());

        assertThat(pushSubscriptionRepository.findByEndpoint("https://example.test/ep")).isPresent();
        assertThat(pushSubscriptionRepository.findAllByUserId("cust-1")).hasSize(1);
    }
}

