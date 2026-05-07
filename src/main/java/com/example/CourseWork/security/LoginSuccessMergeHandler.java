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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String guestId = (String) session.getAttribute(GuestSessionFilter.PREVIOUS_GUEST_ID);
            String authId = oidcUser.getSubject();

            if (guestId != null && authId != null) {
                // Check whether there is an active order on either side
                boolean guestHasActiveOrder = orderService.getMyActiveOrder(guestId).isPresent();
                boolean authHasActiveOrder  = orderService.getMyActiveOrder(authId).isPresent();

                if (!guestHasActiveOrder && !authHasActiveOrder) {
                    // No active orders → safe to merge carts normally
                    cartService.mergeCarts(guestId, authId);
                } else {
                    // An active order exists (on guest and/or auth side).
                    // We will transfer the order via mergeOrders below.
                    // Clear the auth user's cart so it doesn't conflict with the incoming active order.
                    cartService.clearCart(authId);
                    // Also remove the guest cart to avoid it becoming orphaned data.
                    cartService.clearCart(guestId);
                }

                // Transfer all guest orders to the authenticated account
                orderService.mergeOrders(guestId, authId);

                // Clean up the session attribute
                session.removeAttribute(GuestSessionFilter.PREVIOUS_GUEST_ID);
            }
        }

        // Default redirect behavior
        this.setDefaultTargetUrl("/");
        this.setAlwaysUseDefaultTargetUrl(true);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
