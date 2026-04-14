package com.example.CourseWork.controller;

import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.MenuWithDishesDto;
import com.example.CourseWork.service.MenuService;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

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
            HttpSession session,
            Model model) {
        Integer tableNumber = (Integer) session.getAttribute("tableNumber");
        model.addAttribute("tableNumber", tableNumber);

        LocalTime now = LocalTime.now();
        List<MenuWithDishesDto> allActive = menuService.getAllMenusWithDishes().stream()
                .filter(m -> isMenuAvailableNow(m, now))
                .collect(Collectors.toList());

        List<MenuWithDishesDto> menus;
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            menus = allActive;
        } else {
            menus = allActive.stream()
                    .filter(m -> m.getId() != null && m.getId().toString().equals(filter))
                    .collect(Collectors.toList());
        }

        model.addAttribute("menus", menus);
        model.addAttribute("menuView", view);
        model.addAttribute("hasActiveUnpaidOrder", hasActiveUnpaidOrder());
        return "fragments/public-menu-categories :: categoryBodies";
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
}
