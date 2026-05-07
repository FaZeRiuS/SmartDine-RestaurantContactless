package com.example.CourseWork.security;

import com.example.CourseWork.service.cart.CartService;
import com.example.CourseWork.service.order.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessMergeHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final CartService cartService;
    private final OrderService orderService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String guestId = (String) session.getAttribute(GuestSessionFilter.PREVIOUS_GUEST_ID);
            String authId = oidcUser.getSubject();

            if (guestId != null && authId != null) {
                // Merge carts and orders
                cartService.mergeCarts(guestId, authId);
                orderService.mergeOrders(guestId, authId);

                // Clean up the attribute
                session.removeAttribute(GuestSessionFilter.PREVIOUS_GUEST_ID);
            }
        }

        // Default redirect behavior
        this.setDefaultTargetUrl("/");
        this.setAlwaysUseDefaultTargetUrl(true);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
