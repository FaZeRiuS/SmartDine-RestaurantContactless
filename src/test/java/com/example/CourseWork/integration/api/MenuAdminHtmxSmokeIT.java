package com.example.CourseWork.integration.api;

import com.example.CourseWork.security.CurrentUserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class MenuAdminHtmxSmokeIT {

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
    void availableDishes_isPublic() throws Exception {
        mockMvc.perform(get("/api/dishes/available"))
                .andExpect(status().isOk());
    }

    @Test
    void htmxCustomerOrders_requiresCustomer() throws Exception {
        mockMvc.perform(get("/htmx/customer/orders").with(login("chef-1", "CHEF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/htmx/customer/orders").with(login("cust-1", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void htmxAdminOrders_requiresAdmin() throws Exception {
        mockMvc.perform(get("/htmx/admin/orders/table").with(login("chef-1", "CHEF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/htmx/admin/orders/table").with(login("admin-1", "ADMINISTRATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void htmxAdminDishes_allowsChefAndAdmin() throws Exception {
        mockMvc.perform(get("/htmx/admin/dishes/table").with(login("cust-1", "CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/htmx/admin/dishes/table").with(login("chef-1", "CHEF")))
                .andExpect(status().isOk());
    }
}

