package com.example.CourseWork.controller;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.security.CurrentUserIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;
    private final RecommendationService recommendationService;
    private final CurrentUserIdentity currentUserIdentity;

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PostMapping
    public ResponseEntity<DishResponseDto> createDish(@Valid @RequestBody DishDto dto) {
        return ResponseEntity.ok(dishService.createDish(dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PutMapping("/{id}")
    public ResponseEntity<DishResponseDto> updateDish(@PathVariable Integer id, @Valid @RequestBody DishDto dto) {
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
        return ResponseEntity.ok(recommendationService.getRecommendations(currentUserIdentity.currentUserId()));
    }

    @GetMapping("/{id}/smart-combo")
    public ResponseEntity<DishResponseDto> getSmartCombo(
            @PathVariable Integer id,
            @RequestParam(required = false) List<Integer> cartDishIds) {
        Optional<DishResponseDto> combo = dishService.getSmartCombo(id, cartDishIds);
        return combo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
