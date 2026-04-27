package com.example.CourseWork.integration.api;

import com.example.CourseWork.security.CurrentUserIdentity;
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

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class SecurityCsrfSmokeIT {

    @Autowired MockMvc mockMvc;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;
    @MockitoBean CurrentUserIdentity currentUserIdentity;

    @org.springframework.lang.NonNull
    private RequestPostProcessor login(String userId, String role) {
        doReturn(userId).when(currentUserIdentity).currentUserId();
        return oidcLogin()
                .idToken(token -> token.subject(userId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void paymentCallback_doesNotRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("data", "x")
                        .param("signature", "y"))
                .andExpect(status().isBadRequest()); // reaches controller; fails signature/validation
    }

    @Test
    void otherStateChangingEndpoints_requireCsrf() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleGuards_areEnforced() throws Exception {
        mockMvc.perform(get("/api/orders/history").with(login("cust-1", "CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/history").with(login("chef-1", "CHEF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/orders/1/status")
                        .with(login("cust-1", "CUSTOMER"))
                        .with(csrf())
                        .param("newStatus", "PREPARING"))
                .andExpect(status().isForbidden());
    }
}

