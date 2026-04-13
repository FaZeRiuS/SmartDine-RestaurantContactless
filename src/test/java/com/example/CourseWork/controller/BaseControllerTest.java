package com.example.CourseWork.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.springframework.lang.NonNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@AutoConfigureMockMvc
@org.springframework.context.annotation.Import({ com.example.CourseWork.config.SecurityConfig.class,
        com.example.CourseWork.config.SecurityProperties.class })
@org.springframework.test.context.ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockitoBean
    protected org.springframework.security.oauth2.client.userinfo.OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;

    @MockitoBean
    protected CurrentUserIdentity currentUserIdentity;

    @MockitoBean(name = "loyaltyService")
    protected com.example.CourseWork.service.LoyaltyService loyaltyService;

    @BeforeEach
    void setUpBase() {
    }

    @AfterEach
    void tearDownBase() {
    }

    /**
     * Helper to attach user identity (JWT subject + roles).
     */
    @NonNull
    @SuppressWarnings("null")
    protected RequestPostProcessor withUser(String userId, String role) {
        java.util.UUID finalUuid;
        try {
            finalUuid = java.util.UUID.fromString(userId);
        } catch (Exception e) {
            finalUuid = java.util.UUID.nameUUIDFromBytes(userId.getBytes());
        }

        doReturn(userId).when(currentUserIdentity).currentUserId();
        doReturn(finalUuid).when(currentUserIdentity).requireCustomerUuid(anyString());
        doReturn(finalUuid).when(currentUserIdentity).requireCustomerUuid();

        return oidcLogin()
                .idToken(token -> token.claim("preferred_username", userId).subject(userId))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }
}
