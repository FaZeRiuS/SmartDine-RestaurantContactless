package com.example.CourseWork.controller;

import com.example.CourseWork.dto.DishDto;
import com.example.CourseWork.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/htmx/admin/dishes")
@RequiredArgsConstructor
public class HtmxAdminDishesController {

    private final DishService dishService;

    @GetMapping("/table")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String dishesTable(Model model) {
        model.addAttribute("dishes", dishService.getAllDishes());
        return "fragments/admin-dishes-table :: dishesTable";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String upsertDish(
            @RequestParam(required = false) String id,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam String price,
            @RequestParam(required = false) String menuIds,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) String imageUrl,
            Model model,
            HttpServletResponse response) {
        DishDto dto = buildDishDto(name, description, price, menuIds, tags, Boolean.TRUE.equals(isAvailable), imageUrl);
        Integer parsedId = parseOptionalInt(id).orElse(null);
        if (parsedId != null) {
            dishService.updateDish(parsedId, dto);
        } else {
            dishService.createDish(dto);
        }
        model.addAttribute("dishes", dishService.getAllDishes());
        response.setHeader("HX-Trigger", "admin:closeDishModal");
        return "fragments/admin-dishes-table :: dishesTable";
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String updateDish(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam String price,
            @RequestParam(required = false) String menuIds,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) String imageUrl,
            Model model) {
        DishDto dto = buildDishDto(name, description, price, menuIds, tags, Boolean.TRUE.equals(isAvailable), imageUrl);
        dishService.updateDish(id, dto);
        model.addAttribute("dishes", dishService.getAllDishes());
        return "fragments/admin-dishes-table :: dishesTable";
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String deleteDish(@PathVariable Integer id, Model model) {
        dishService.deleteDish(id);
        model.addAttribute("dishes", dishService.getAllDishes());
        return "fragments/admin-dishes-table :: dishesTable";
    }

    private static DishDto buildDishDto(
            String name,
            String description,
            String priceRaw,
            String menuIdsCsv,
            String tagsCsv,
            boolean isAvailable,
            String imageUrl) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введіть назву страви");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceRaw.trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некоректна ціна");
        }

        DishDto dto = new DishDto();
        dto.setName(name.trim());
        dto.setDescription(description != null ? description.trim() : "");
        dto.setPrice(price);
        dto.setIsAvailable(isAvailable);
        dto.setImageUrl(imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null);
        dto.setMenuIds(parseMenuIds(menuIdsCsv));
        dto.setTags(parseTags(tagsCsv));
        return dto;
    }

    private static List<Integer> parseMenuIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return out;
    }

    private static List<String> parseTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static Optional<Integer> parseOptionalInt(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.trim();
        if (s.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
