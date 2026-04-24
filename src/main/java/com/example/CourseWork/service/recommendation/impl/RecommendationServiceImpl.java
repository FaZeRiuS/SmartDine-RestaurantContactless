package com.example.CourseWork.service.recommendation.impl;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.menu.DishRatingService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import com.example.CourseWork.service.user.UserPreferenceService;
import com.example.CourseWork.security.CurrentUserIdentity;
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
    private final CurrentUserIdentity currentUserIdentity;

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
                .collect(Collectors.toMap(Dish::getId, d -> d.getAllergens() == null ? List.of() : d.getAllergens(), (a, b) -> a));
        Map<Integer, List<String>> tagsById = dishRepository.findAllByIdWithTags(ids).stream()
                .filter(d -> d != null && d.getId() != null)
                .collect(Collectors.toMap(Dish::getId, d -> d.getTags() == null ? List.of() : d.getTags(), (a, b) -> a));

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

        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DishResponseDto> getCrossSellRecommendation(Integer baseDishId, List<Integer> existingDishIds) {
        List<Integer> existing = existingDishIds != null ? existingDishIds : List.of();

        List<Object[]> rows = dishRepository.findSmartComboCandidatesForDish(baseDishId, SMART_COMBO_CANDIDATE_LIMIT);
        if (rows.isEmpty()) {
            rows = dishRepository.findPopularComboFallbackCandidates(baseDishId, SMART_COMBO_CANDIDATE_LIMIT);
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
            if (!existing.isEmpty() && dishRepository.checkIfSharesMenu(id, existing)) {
                continue;
            }
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            weighted.add(new long[]{id, cnt + 1L});
        }

        // Filter out dishes with excluded allergens for the current authenticated user (if any).
        String userId = currentUserIdentity.currentUserId();
        java.util.Set<String> excluded = userPreferenceService.getExcludedAllergens(userId);
        if (!excluded.isEmpty() && !weighted.isEmpty()) {
            List<Integer> candidateIds = weighted.stream().map(e -> (int) e[0]).distinct().toList();
            java.util.Set<Integer> disallowedIds = dishRepository.findAllByIdWithAllergens(candidateIds).stream()
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
