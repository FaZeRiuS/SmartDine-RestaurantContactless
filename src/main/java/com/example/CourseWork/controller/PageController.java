package com.example.CourseWork.controller;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.dto.MenuWithDishesDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.dto.CartItemDetailDto;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.DishService;
import com.example.CourseWork.service.MenuService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.RecommendationService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private final MenuService menuService;
    private final DishService dishService;
    private final RecommendationService recommendationService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUserIdentity currentUserIdentity;
    private final String keycloakPublicUrl;

    public PageController(
            MenuService menuService,
            DishService dishService,
            RecommendationService recommendationService,
            CartService cartService,
            OrderService orderService,
            CurrentUserIdentity currentUserIdentity,
            @Value("${keycloak.public-url:http://localhost:8080}") String keycloakPublicUrl
    ) {
        this.menuService = menuService;
        this.dishService = dishService;
        this.recommendationService = recommendationService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.currentUserIdentity = currentUserIdentity;
        this.keycloakPublicUrl = keycloakPublicUrl;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) Integer table,
                        HttpSession session,
                        Model model) {
        if (table != null) {
            session.setAttribute("tableNumber", table);
        }
        Integer sessionTable = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", sessionTable);

        LocalTime now = LocalTime.now();
        List<MenuWithDishesDto> allMenus = menuService.getAllMenusWithDishes();

        // Filter menus by time
        List<MenuWithDishesDto> activeMenus = allMenus.stream()
                .filter(m -> isMenuAvailableNow(m, now))
                .collect(Collectors.toList());

        model.addAttribute("menus", activeMenus);

        // 1. Get Popular Dishes (General for all)
        List<DishResponseDto> allAvailable = dishService.getAllAvailableDishes();
        List<DishResponseDto> popularDishes = allAvailable.stream()
                .limit(6)
                .collect(Collectors.toList());

        // 2. Get Personalized Recommendations (Only for matched users)
        List<DishResponseDto> personalized = List.of();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            String userId = oidcUser.getSubject();
            personalized = recommendationService.getRecommendations(userId);
            if (personalized.size() > 6) {
                personalized = personalized.subList(0, 6);
            }

            // Deduplicate: remove personalized items from popular list for this view
            java.util.Set<Integer> personalizedIds = personalized.stream()
                    .map(com.example.CourseWork.dto.DishResponseDto::getId)
                    .collect(java.util.stream.Collectors.toSet());

            popularDishes = allAvailable.stream()
                    .filter(d -> !personalizedIds.contains(d.getId()))
                    .limit(6)
                    .collect(Collectors.toList());
        }

        model.addAttribute("personalizedRecommendations", personalized);
        model.addAttribute("popularDishes", popularDishes);
        model.addAttribute("lcpPreloadImageUrl", resolveHomeLcpPreloadImageUrl(personalized, popularDishes, auth));
        model.addAttribute("menuView", "index");
        model.addAttribute("hasActiveUnpaidOrder", hasActiveUnpaidOrder());

        return "index";
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(required = false) Integer id, 
                       HttpSession session, 
                       Model model) {
        Integer sessionTable = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", sessionTable);

        LocalTime now = LocalTime.now();
        List<MenuWithDishesDto> allMenus = menuService.getAllMenusWithDishes();

        // Always show filtered menu on public page
        List<MenuWithDishesDto> displayMenus = allMenus.stream()
                .filter(m -> isMenuAvailableNow(m, now))
                .collect(Collectors.toList());

        model.addAttribute("menus", displayMenus);
        model.addAttribute("selectedMenuId", id);

        List<MenuWithDishesDto> menusForBody;
        if (id != null) {
            menusForBody = displayMenus.stream()
                    .filter(m -> id.equals(m.getId()))
                    .collect(Collectors.toList());
        } else {
            menusForBody = displayMenus;
        }
        model.addAttribute("menusForBody", menusForBody);
        model.addAttribute("menuView", "menu");
        model.addAttribute("hasActiveUnpaidOrder", hasActiveUnpaidOrder());

        return "menu";
    }

    /**
     * First dish image shown above the fold on home: personalized block (if signed in), else popular
     * (hidden for staff roles — must match index.html sec:authorize rules).
     */
    private static String resolveHomeLcpPreloadImageUrl(
            List<DishResponseDto> personalized,
            List<DishResponseDto> popularDishes,
            Authentication auth) {
        if (isSignedIn(auth)) {
            String fromPersonalized = firstLcpPreloadUrl(personalized);
            if (fromPersonalized != null) {
                return fromPersonalized;
            }
        }
        if (!isStaffRole(auth)) {
            return firstLcpPreloadUrl(popularDishes);
        }
        return null;
    }

    private static boolean isSignedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }

    private static boolean isStaffRole(Authentication auth) {
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> "ROLE_WAITER".equals(r) || "ROLE_CHEF".equals(r) || "ROLE_ADMINISTRATOR".equals(r));
    }

    /** Prefer medium derivative for LCP byte savings; fall back to small then master. */
    private static String firstLcpPreloadUrl(List<DishResponseDto> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return null;
        }
        for (DishResponseDto d : dishes) {
            String u = preferredLcpUrlForDish(d);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    private static String preferredLcpUrlForDish(DishResponseDto d) {
        if (d == null) {
            return null;
        }
        if (d.getImageUrlMedium() != null && !d.getImageUrlMedium().isBlank()) {
            return d.getImageUrlMedium();
        }
        if (d.getImageUrlSmall() != null && !d.getImageUrlSmall().isBlank()) {
            return d.getImageUrlSmall();
        }
        if (d.getImageUrl() != null && !d.getImageUrl().isBlank()) {
            return d.getImageUrl();
        }
        return null;
    }

    private boolean hasActiveUnpaidOrder() {
        return orderService.getMyActiveOrder(currentUserIdentity.currentUserId())
                .filter(o -> o.getPaymentStatus() != PaymentStatus.SUCCESS)
                .isPresent();
    }

    private boolean isMenuAvailableNow(MenuWithDishesDto menu, LocalTime now) {
        if (menu.getStartTime() == null || menu.getEndTime() == null) {
            return true;
        }
        return !now.isBefore(menu.getStartTime()) && !now.isAfter(menu.getEndTime());
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", tableNumber);

        CartResponseDto cart = cartService.getCartByUserId(currentUserIdentity.currentUserId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cartCount(cart));
        model.addAttribute("cartTotal", cartTotal(cart));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("userId", oidcUser.getSubject());
        }
        return "cart";
    }

    private static int cartCount(CartResponseDto cart) {
        if (cart == null || cart.getItems() == null) return 0;
        return cart.getItems().stream()
                .filter(i -> i != null && i.getQuantity() != null)
                .mapToInt(CartItemDetailDto::getQuantity)
                .sum();
    }

    private static BigDecimal cartTotal(CartResponseDto cart) {
        if (cart == null || cart.getItems() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .filter(i -> i != null && i.getPrice() != null && i.getQuantity() != null)
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        return "orders";
    }

    @GetMapping("/profile")
    public String profile() {
        // Direct redirect to Keycloak Account Console for the specific realm
        return "redirect:" + keycloakPublicUrl + "/realms/restaurant-realm/account";
    }

    @GetMapping("/staff/orders")
    @PreAuthorize("hasAnyRole('WAITER','CHEF','ADMINISTRATOR')")
    public String staffOrders(Model model) {
        return "staff/orders";
    }

    @GetMapping("/admin/menu")
    @PreAuthorize("hasAnyRole('CHEF','ADMINISTRATOR')")
    public String adminMenu() {
        return "admin/menu-editor";
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String adminOrders(Model model) {
        return "admin/orders-history";
    }
}
