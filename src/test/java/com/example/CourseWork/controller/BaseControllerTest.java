package com.example.CourseWork.controller;

import com.example.CourseWork.util.KeycloakUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.springframework.lang.NonNull;
import java.util.Arrays;

import static org.mockito.Mockito.mockStatic;

@AutoConfigureMockMvc
@org.springframework.context.annotation.Import({com.example.CourseWork.config.SecurityConfig.class, com.example.CourseWork.config.SecurityProperties.class})
@org.springframework.test.context.ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockitoBean
    protected org.springframework.security.oauth2.client.userinfo.OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;

    @MockitoBean(name = "loyaltyService")
    protected com.example.CourseWork.service.LoyaltyService loyaltyService;

    protected MockedStatic<KeycloakUtil> keycloakUtilMockedStatic;

    @BeforeEach
    void setUpBase() {
        keycloakUtilMockedStatic = mockStatic(KeycloakUtil.class);
    }

    @AfterEach
    void tearDownBase() {
        keycloakUtilMockedStatic.close();
    }

    /**
     * Helper to mock Keycloak user in SecurityContext and KeycloakUtil
     */
    @NonNull
    @SuppressWarnings("null")
    protected RequestPostProcessor withUser(String userId, String... roles) {
        com.example.CourseWork.model.KeycloakUser mockUser = new com.example.CourseWork.model.KeycloakUser();
        mockUser.setId(userId);
        mockUser.setUsername("testuser");
        
        keycloakUtilMockedStatic.when(KeycloakUtil::getCurrentUser).thenReturn(mockUser);

        java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(java.util.stream.Collectors.toList());

        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("sub", userId))
                .authorities(authorities);
    }
}
