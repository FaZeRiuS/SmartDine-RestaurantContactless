package com.example.CourseWork.service.recommendation.impl;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.menu.DishRatingService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.service.user.UserPreferenceService;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int SMART_COMBO_CANDIDATE_LIMIT = 10;

    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final DishRatingService dishRatingService;
    private final UserPreferenceService userPreferenceService;

    @Cacheable(cacheNames = "personalizedRecommendations", key = "#userId")
    @Override
    @Transactional(readOnly = true)
    public List<DishResponseDto> getRecommendations(String userId) {
        java.util.Set<String> excluded = userPreferenceService.getExcludedAllergens(userId);

        List<Dish> base = dishRepository.findRecommendedDishes(userId);
        if (base.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = base.stream().map(Dish::getId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Integer, List<String>> allergensById = dishRepository.findAllByIdWithAllergens(ids).stream()
                .filter(d -> d != null && d.getId() != null)
                .collect(Collectors.toMap(Dish::getId, d -> d.getAllergens() == null ? List.of() : d.getAllergens(),
                        (a, b) -> a));
        Map<Integer, List<String>> tagsById = dishRepository.findAllByIdWithTags(ids).stream()
                .filter(d -> d != null && d.getId() != null)
                .collect(
                        Collectors.toMap(Dish::getId, d -> d.getTags() == null ? List.of() : d.getTags(), (a, b) -> a));

        List<DishResponseDto> dishes = base.stream()
                .map(dishMapper::toResponseDto)
                .peek(dto -> {
                    if (dto.getId() != null) {
                        dto.setAllergens(allergensById.getOrDefault(dto.getId(), List.of()));
                        dto.setTags(tagsById.getOrDefault(dto.getId(), List.of()));
                    }
                })
                .filter(dto -> excluded.isEmpty()
                        || dto.getAllergens() == null
                        || dto.getAllergens().stream().noneMatch(excluded::contains))
                .collect(Collectors.toList());

        if (dishes.size() > 6) {
            dishes = dishes.subList(0, 6);
        }
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DishResponseDto> getCrossSellRecommendation(String userId, Integer baseDishId,
            List<Integer> existingDishIds) {
        List<Integer> existing = existingDishIds != null ? existingDishIds : List.of();

        List<Object[]> rows = dishRepository.findSmartComboCandidatesForDish(baseDishId, SMART_COMBO_CANDIDATE_LIMIT);
        if (rows.isEmpty()) {
            rows = dishRepository.findPopularComboFallbackCandidates(baseDishId, SMART_COMBO_CANDIDATE_LIMIT);
        }

        List<Integer> candidateIds = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 1 && row[0] != null) {
                candidateIds.add(((Number) row[0]).intValue());
            }
        }

        // Single batch fetch of all menu links for candidate and existing dishes to
        // prevent N+1 queries (Issue 14)
        List<Integer> allIds = new java.util.ArrayList<>(candidateIds);
        allIds.addAll(existing);

        Map<Integer, java.util.Set<Integer>> dishMenusMap = new java.util.HashMap<>();
        if (!allIds.isEmpty()) {
            List<Object[]> links = dishRepository.findDishMenuLinksForDishIds(allIds);
            for (Object[] link : links) {
                if (link != null && link.length >= 2 && link[0] != null && link[1] != null) {
                    int dishId = ((Number) link[0]).intValue();
                    int menuId = ((Number) link[1]).intValue();
                    dishMenusMap.computeIfAbsent(dishId, k -> new java.util.HashSet<>()).add(menuId);
                }
            }
        }

        java.util.Set<Integer> existingMenuIds = new java.util.HashSet<>();
        for (Integer exId : existing) {
            java.util.Set<Integer> menus = dishMenusMap.get(exId);
            if (menus != null) {
                existingMenuIds.addAll(menus);
            }
        }

        List<long[]> weighted = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            int id = ((Number) row[0]).intValue();
            if (existing.contains(id)) {
                continue;
            }

            // Check menu sharing in memory (Issue 14)
            java.util.Set<Integer> candidateMenus = dishMenusMap.getOrDefault(id, java.util.Set.of());
            boolean sharesMenu = candidateMenus.stream().anyMatch(existingMenuIds::contains);
            if (sharesMenu) {
                continue;
            }

            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            weighted.add(new long[] { id, cnt + 1L });
        }

        // Filter out dishes with excluded allergens for the user safely (Issue 4)
        java.util.Set<String> excluded = (userId != null && !userId.isBlank())
                ? userPreferenceService.getExcludedAllergens(userId)
                : java.util.Set.of();
        if (!excluded.isEmpty() && !weighted.isEmpty()) {
            List<Integer> finalCandidateIds = weighted.stream().map(e -> (int) e[0]).distinct().toList();
            java.util.Set<Integer> disallowedIds = dishRepository.findAllByIdWithAllergens(finalCandidateIds).stream()
                    .filter(d -> d != null && d.getId() != null && d.getAllergens() != null
                            && d.getAllergens().stream().anyMatch(excluded::contains))
                    .map(Dish::getId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!disallowedIds.isEmpty()) {
                weighted = weighted.stream()
                        .filter(e -> !disallowedIds.contains((int) e[0]))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            }
        }

        Optional<DishResponseDto> dish = pickWeightedId(weighted)
                .flatMap(dishRepository::findById)
                .map(dishMapper::toResponseDto);
        dish.ifPresent(d -> dishRatingService.enrichWithRatings(List.of(d)));
        return dish;
    }

    private static Optional<Integer> pickWeightedId(List<long[]> weighted) {
        if (weighted.isEmpty()) {
            return Optional.empty();
        }
        long sum = 0;
        for (long[] e : weighted) {
            sum += e[1];
        }
        if (sum <= 0) {
            int idx = ThreadLocalRandom.current().nextInt(weighted.size());
            return Optional.of((int) weighted.get(idx)[0]);
        }
        long r = ThreadLocalRandom.current().nextLong(sum) + 1;
        long acc = 0;
        for (long[] e : weighted) {
            acc += e[1];
            if (r <= acc) {
                return Optional.of((int) e[0]);
            }
        }
        return Optional.of((int) weighted.get(weighted.size() - 1)[0]);
    }
}
