package com.example.CourseWork.controller;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.dto.cart.CartResponseDto;
import com.example.CourseWork.dto.cart.CartItemDetailDto;
import com.example.CourseWork.service.cart.CartService;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.menu.MenuService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.security.CurrentUserIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.net.URI;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private final MenuService menuService;
    private final DishService dishService;
    private final RecommendationService recommendationService;
    private final CartService cartService;
    private final CurrentUserIdentity currentUserIdentity;
    private final String keycloakPublicUrl;
    private final Clock appClock;

    public PageController(
            MenuService menuService,
            DishService dishService,
            RecommendationService recommendationService,
            CartService cartService,
            CurrentUserIdentity currentUserIdentity,
            Clock appClock,
            @Value("${keycloak.public-url:http://localhost:8080}") String keycloakPublicUrl
    ) {
        this.menuService = menuService;
        this.dishService = dishService;
        this.recommendationService = recommendationService;
        this.cartService = cartService;
        this.currentUserIdentity = currentUserIdentity;
        this.appClock = appClock;
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

        LocalTime now = LocalTime.now(appClock).truncatedTo(ChronoUnit.MINUTES);
        List<MenuWithDishesDto> allMenus = menuService.getAllMenusWithDishes();

        // Filter menus by time
        List<MenuWithDishesDto> activeMenus = allMenus.stream()
                .filter(m -> isMenuAvailableNow(m, now))
                .collect(Collectors.toList());

        model.addAttribute("menus", activeMenus);

        // 1. Personalized recommendations (logged-in customers only)
        List<DishResponseDto> personalized = List.of();
        java.util.Set<Integer> excludeFromPopular = java.util.Collections.emptySet();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            String userId = oidcUser.getSubject();
            personalized = recommendationService.getRecommendations(userId);
            if (personalized.size() > 6) {
                personalized = personalized.subList(0, 6);
            }
            excludeFromPopular = personalized.stream()
                    .map(DishResponseDto::getId)
                    .collect(Collectors.toSet());
        }

        // 2. Popular dishes: highest total ordered quantity, max 2 per menu, up to 6
        List<DishResponseDto> popularDishes = dishService.getPopularDishesForHome(6, 2, excludeFromPopular);

        model.addAttribute("personalizedRecommendations", personalized);
        model.addAttribute("popularDishes", popularDishes);
        model.addAttribute("menuView", "index");

        // Performance hints: help browser start LCP image request earlier (preconnect/preload).
        // LCP on this page is typically the first dish image.
        DishResponseDto lcpDish = firstDishForLcp(personalized, popularDishes);
        if (lcpDish != null && lcpDish.getImageUrl() != null && !lcpDish.getImageUrl().isBlank()) {
            String lcpImageUrl = lcpDish.getImageUrl();
            model.addAttribute("lcpImageUrl", lcpImageUrl);
            String origin = extractOrigin(lcpImageUrl);
            if (origin != null) {
                model.addAttribute("lcpImagePreconnectOrigin", origin);
            }
            model.addAttribute("lcpImageCrossorigin", isAbsoluteHttpUrl(lcpImageUrl));
        }

        return "index";
    }

    private static DishResponseDto firstDishForLcp(List<DishResponseDto> personalized, List<DishResponseDto> popular) {
        DishResponseDto d = firstWithImage(personalized, 3);
        if (d != null) return d;
        return firstWithImage(popular, 3);
    }

    private static DishResponseDto firstWithImage(List<DishResponseDto> dishes, int limit) {
        if (dishes == null || dishes.isEmpty()) return null;
        int max = Math.min(limit, dishes.size());
        for (int i = 0; i < max; i++) {
            DishResponseDto d = dishes.get(i);
            if (d != null && d.getImageUrl() != null && !d.getImageUrl().isBlank()) return d;
        }
        return null;
    }

    private static boolean isAbsoluteHttpUrl(String url) {
        if (url == null) return false;
        String u = url.trim();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    private static String extractOrigin(String url) {
        if (!isAbsoluteHttpUrl(url)) return null;
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            int port = uri.getPort();
            return port == -1 ? (uri.getScheme() + "://" + uri.getHost())
                    : (uri.getScheme() + "://" + uri.getHost() + ":" + port);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(required = false) Integer id, 
                       HttpSession session, 
                       Model model) {
        Integer sessionTable = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", sessionTable);

        LocalTime now = LocalTime.now(appClock).truncatedTo(ChronoUnit.MINUTES);
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

        return "menu";
    }

    private boolean isMenuAvailableNow(MenuWithDishesDto menu, LocalTime now) {
        if (menu.getStartTime() == null || menu.getEndTime() == null) {
            return true;
        }
        // Support menus that cross midnight (e.g., 18:00 — 02:00)
        if (menu.getStartTime().isBefore(menu.getEndTime())) {
            return !now.isBefore(menu.getStartTime()) && !now.isAfter(menu.getEndTime());
        }
        return !now.isBefore(menu.getStartTime()) || !now.isAfter(menu.getEndTime());
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
