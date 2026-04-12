package com.example.CourseWork.controller;

import com.example.CourseWork.dto.DishDto;
import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.service.DishService;
import com.example.CourseWork.service.RecommendationService;
import com.example.CourseWork.util.KeycloakUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;
    private final RecommendationService recommendationService;

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PostMapping
    public ResponseEntity<DishResponseDto> createDish(@RequestBody DishDto dto) {
        return ResponseEntity.ok(dishService.createDish(dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PutMapping("/{id}")
    public ResponseEntity<DishResponseDto> updateDish(@PathVariable Integer id, @RequestBody DishDto dto) {
        return ResponseEntity.ok(dishService.updateDish(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Integer id) {
        dishService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<DishResponseDto>> getAvailableDishes() {
        return ResponseEntity.ok(dishService.getAllAvailableDishes());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    public ResponseEntity<List<DishResponseDto>> getAllDishes() {
        return ResponseEntity.ok(dishService.getAllDishes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishResponseDto> getDishById(@PathVariable Integer id) {
        return ResponseEntity.ok(dishService.getDishById(id));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<DishResponseDto>> getRecommendations() {
        return ResponseEntity.ok(recommendationService.getRecommendations(KeycloakUtil.getCurrentUser().getId()));
    }

    @GetMapping("/{id}/smart-combo")
    public ResponseEntity<DishResponseDto> getSmartCombo(
            @PathVariable Integer id,
            @RequestParam(required = false) List<Integer> cartDishIds) {
        DishResponseDto combo = dishService.getSmartCombo(id, cartDishIds);
        return combo != null ? ResponseEntity.ok(combo) : ResponseEntity.noContent().build();
    }
}
