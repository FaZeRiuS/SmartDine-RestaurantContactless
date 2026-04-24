package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.service.menu.MenuService;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.security.CurrentUserIdentity;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * Public HTMX fragments for menu category bodies (index + full menu page).
 */
@Controller
@RequestMapping("/htmx/menu")
@RequiredArgsConstructor
public class HtmxMenuCategoriesController {

    private final MenuService menuService;
    private final OrderService orderService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping("/categories-body")
    public String categoriesBody(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "index") String view,
            @RequestParam(required = false) List<String> includeTags,
            @RequestParam(required = false) List<String> excludeTags,
            @RequestParam(required = false) List<String> excludeAllergens,
            HttpSession session,
            Model model) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", tableNumber);

        List<MenuWithDishesDto> menus = menuService.getActiveMenusWithDishes(
                filter,
                includeTags == null ? List.of() : includeTags,
                excludeTags == null ? List.of() : excludeTags,
                excludeAllergens == null ? List.of() : excludeAllergens
        );

        model.addAttribute("menus", menus);
        model.addAttribute("menuView", view);

        Optional<OrderResponseDto> activeOrderOpt = orderService.getMyActiveOrder(currentUserIdentity.currentUserId());
        boolean hasActivePaidOrder = activeOrderOpt.isPresent()
                && PaymentStatus.SUCCESS.equals(activeOrderOpt.get().getPaymentStatus());
        model.addAttribute("hasActivePaidOrder", hasActivePaidOrder);

        return "fragments/public-menu-categories :: categoryBodies";
    }
}
