package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.security.CurrentUserIdentity;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.service.user.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/htmx/home")
@RequiredArgsConstructor
public class HtmxHomeDishesController {

    private final DishService dishService;
    private final RecommendationService recommendationService;
    private final UserPreferenceService userPreferenceService;
    private final CurrentUserIdentity currentUserIdentity;
    private final OrderService orderService;

    @GetMapping("/popular")
    public String popular(Model model) {
        String userId = currentUserIdentity.currentUserId();
        Set<String> excludedAllergens = userPreferenceService.getExcludedAllergens(userId);

        Set<Integer> excludeIds = Set.of();
        try {
            List<DishResponseDto> rec = recommendationService.getRecommendations(userId);
            excludeIds = rec.stream().map(DishResponseDto::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        } catch (Exception ignored) { /* ignore */ }

        List<DishResponseDto> popular = dishService.getPopularDishesForHome(6, 2, excludeIds, excludedAllergens);
        model.addAttribute("popularDishes", popular);
        model.addAttribute("hasActivePaidOrder", hasActivePaidOrder(userId));
        return "fragments/home-dishes :: popularGrid";
    }

    @GetMapping("/recommendations")
    public String recommendations(Model model) {
        String userId = currentUserIdentity.currentUserId();
        List<DishResponseDto> rec = List.of();
        try {
            rec = recommendationService.getRecommendations(userId);
        } catch (Exception ignored) { /* ignore */ }
        model.addAttribute("personalizedRecommendations", rec);
        model.addAttribute("hasActivePaidOrder", hasActivePaidOrder(userId));
        return "fragments/home-dishes :: recommendationsGrid";
    }

    private boolean hasActivePaidOrder(String userId) {
        try {
            var activeOrderOpt = orderService.getMyActiveOrder(userId);
            return activeOrderOpt.isPresent()
                    && com.example.CourseWork.model.PaymentStatus.SUCCESS.equals(activeOrderOpt.get().getPaymentStatus());
        } catch (Exception e) {
            return false;
        }
    }
}
