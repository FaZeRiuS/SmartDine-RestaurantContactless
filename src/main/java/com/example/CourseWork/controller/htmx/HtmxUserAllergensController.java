package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.security.CurrentUserIdentity;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.user.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/htmx/user/allergens")
@RequiredArgsConstructor
public class HtmxUserAllergensController {

    private final DishService dishService;
    private final UserPreferenceService userPreferenceService;
    private final CurrentUserIdentity currentUserIdentity;

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/modal")
    public String modal(Model model) {
        String userId = currentUserIdentity.currentUserId();
        List<String> availableAllergens = dishService.getDistinctAllergens();
        Set<String> selected = userPreferenceService.getExcludedAllergens(userId);

        model.addAttribute("availableAllergens", availableAllergens);
        model.addAttribute("selectedAllergens", selected);
        return "fragments/user-allergens-modal :: modalBody";
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public String save(@RequestParam(required = false) List<String> excludeAllergens, Model model) {
        String userId = currentUserIdentity.currentUserId();
        userPreferenceService.setExcludedAllergens(userId, excludeAllergens);
        return modal(model);
    }
}

