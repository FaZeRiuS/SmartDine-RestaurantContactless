package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.DishRatingService;
import com.example.CourseWork.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public List<DishResponseDto> getRecommendations(String userId) {
        List<DishResponseDto> dishes = dishRepository.findRecommendedDishes(userId).stream()
                .map(dishMapper::toResponseDto)
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

        Optional<Integer> chosenId = pickWeightedId(weighted);
        if (chosenId.isEmpty()) {
            return Optional.empty();
        }

        return dishRepository.findById(chosenId.get())
                .map(dishMapper::toResponseDto)
                .map(d -> {
                    dishRatingService.enrichWithRatings(List.of(d));
                    return d;
                });
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
