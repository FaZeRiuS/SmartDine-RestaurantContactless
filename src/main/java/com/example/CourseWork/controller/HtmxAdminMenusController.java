package com.example.CourseWork.controller;

import com.example.CourseWork.dto.MenuDto;
import com.example.CourseWork.service.MenuService;
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

import java.time.LocalTime;
import java.util.Optional;

@Controller
@RequestMapping("/htmx/admin/menus")
@RequiredArgsConstructor
public class HtmxAdminMenusController {

    private final MenuService menuService;

    @GetMapping("/table")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String menusTable(Model model) {
        model.addAttribute("menus", menuService.getAllMenusWithDishes());
        return "fragments/admin-menus-table :: menusTable";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String upsertMenu(
            @RequestParam(required = false) String id,
            @RequestParam String name,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model,
            HttpServletResponse response) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введіть назву меню");
        }
        Integer parsedId = parseOptionalInt(id).orElse(null);
        MenuDto dto = new MenuDto();
        dto.setName(name.trim());
        dto.setStartTime(parseTime(startTime));
        dto.setEndTime(parseTime(endTime));
        if (parsedId != null) {
            menuService.updateMenu(parsedId, dto);
        } else {
            menuService.createMenu(dto);
        }
        model.addAttribute("menus", menuService.getAllMenusWithDishes());
        response.setHeader("HX-Trigger", "admin:closeMenuModal");
        return "fragments/admin-menus-table :: menusTable";
    }

    // PUT endpoint kept for backward compatibility with any older clients.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String updateMenu(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введіть назву меню");
        }
        MenuDto dto = new MenuDto();
        dto.setName(name.trim());
        dto.setStartTime(parseTime(startTime));
        dto.setEndTime(parseTime(endTime));
        menuService.updateMenu(id, dto);
        model.addAttribute("menus", menuService.getAllMenusWithDishes());
        return "fragments/admin-menus-table :: menusTable";
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public String deleteMenu(@PathVariable Integer id, Model model) {
        menuService.deleteMenu(id);
        model.addAttribute("menus", menuService.getAllMenusWithDishes());
        return "fragments/admin-menus-table :: menusTable";
    }

    private static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalTime.parse(s);
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
