package com.example.CourseWork.advice;

import com.example.CourseWork.service.LoyaltyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GlobalControllerAdviceTest {

    @Mock(name = "loyaltyService")
    private LoyaltyService loyaltyService;
    @Mock
    private Model model;
    @Mock
    private HttpServletRequest request;
    @Mock
    private com.example.CourseWork.service.security.CurrentUserIdentity currentUserIdentity;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private OidcUser oidcUser;

    private GlobalControllerAdvice globalControllerAdvice;
    
    @BeforeEach
    void setUp() {
        globalControllerAdvice = new GlobalControllerAdvice(loyaltyService, currentUserIdentity);
        ReflectionTestUtils.setField((Object) globalControllerAdvice, "vapidPublicKey", "test-vapid-key");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addAttributes_ShouldAddBasicAttributes_Always() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/menu");
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        globalControllerAdvice.addAttributes(model, request);

        // Assert
        verify(model).addAttribute("currentUri", "/menu");
        verify(model).addAttribute("vapidPublicKey", "test-vapid-key");
    }

    @Test
    void addAttributes_ShouldAddUserAttributes_WhenAuthenticatedAsCustomer() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(request.getRequestURI()).thenReturn("/profile");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getPreferredUsername()).thenReturn("test-user");
        
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .when(authentication).getAuthorities();

        when(currentUserIdentity.requireCustomerUuid(anyString())).thenReturn(userId);
        when(loyaltyService.getBalance(any(UUID.class))).thenReturn(BigDecimal.valueOf(100.50));
        when(loyaltyService.resolveCashbackRate(any(UUID.class))).thenReturn(new BigDecimal("0.10"));

        // Act
        globalControllerAdvice.addAttributes(model, request);

        // Assert
        verify(model).addAttribute("username", "test-user");
        verify(model).addAttribute("loyaltyBalance", BigDecimal.valueOf(100.50));
        verify(model).addAttribute("cashbackRate", new BigDecimal("0.10"));
    }

    @Test
    void addAttributes_ShouldFallbackToName_WhenUsernameIsEmpty() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getPreferredUsername()).thenReturn(null);
        when(oidcUser.getName()).thenReturn("sub-123");
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        // Act
        globalControllerAdvice.addAttributes(model, request);

        // Assert
        verify(model).addAttribute("username", "sub-123");
    }
}
