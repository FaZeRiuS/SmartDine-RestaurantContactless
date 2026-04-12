package com.example.CourseWork.util;

import com.example.CourseWork.model.KeycloakUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakUtilTest {

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @Mock private Jwt jwt;
    @Mock private OidcUser oidcUser;
    @Mock private ServletRequestAttributes requestAttributes;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    private MockedStatic<RequestContextHolder> mockedRequestContextHolder;

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class);
        mockedRequestContextHolder = Mockito.mockStatic(RequestContextHolder.class);
        
        lenient().when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
        mockedRequestContextHolder.close();
    }

    @Test
    void getCurrentUser_WithJwt_ShouldReturnUser() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("jwt-id-123");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("jwt-user");
        when(jwt.getClaimAsString("email")).thenReturn("jwt@example.com");

        // Act
        KeycloakUser result = KeycloakUtil.getCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("jwt-id-123");
        assertThat(result.getUsername()).isEqualTo("jwt-user");
        assertThat(result.getEmail()).isEqualTo("jwt@example.com");
    }

    @Test
    void getCurrentUser_WithOidc_ShouldReturnUser() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getSubject()).thenReturn("oidc-id-456");
        when(oidcUser.getPreferredUsername()).thenReturn("oidc-user");
        when(oidcUser.getEmail()).thenReturn("oidc@example.com");

        // Act
        KeycloakUser result = KeycloakUtil.getCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("oidc-id-456");
        assertThat(result.getUsername()).isEqualTo("oidc-user");
    }

    @Test
    void getCurrentUser_AsGuest_ShouldReturnGuestWithSessionId() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        when(RequestContextHolder.currentRequestAttributes()).thenReturn(requestAttributes);
        when(requestAttributes.getSessionId()).thenReturn("session-abc");

        // Act
        KeycloakUser result = KeycloakUtil.getCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("GUEST_session-abc");
        assertThat(result.getUsername()).isEqualTo("Гість");
    }

    @Test
    void getCurrentUserOrNull_WithNoAuth_ShouldReturnNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        KeycloakUser result = KeycloakUtil.getCurrentUserOrNull();

        // Assert
        assertThat(result).isNull();
    }
}
