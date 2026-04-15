package com.example.CourseWork.controller;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.dto.MenuWithDishesDto;
import com.example.CourseWork.dto.CartResponseDto;
import com.example.CourseWork.dto.CartItemDetailDto;
import com.example.CourseWork.service.CartService;
import com.example.CourseWork.service.DishService;
import com.example.CourseWork.service.MenuService;
import com.example.CourseWork.service.RecommendationService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
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
    private final CurrentUserIdentity currentUserIdentity;
    private final String keycloakPublicUrl;

    public PageController(
            MenuService menuService,
            DishService dishService,
            RecommendationService recommendationService,
            CartService cartService,
            CurrentUserIdentity currentUserIdentity,
            @Value("${keycloak.public-url:http://localhost:8080}") String keycloakPublicUrl
    ) {
        this.menuService = menuService;
        this.dishService = dishService;
        this.recommendationService = recommendationService;
        this.cartService = cartService;
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
        model.addAttribute("menuView", "index");

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

        return "menu";
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
