package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.service.DishRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishRatingServiceImpl implements DishRatingService {

    private final OrderDishReviewRepository orderDishReviewRepository;

    @Override
    public void enrichWithRatings(List<DishResponseDto> dishes) {
        if (dishes == null || dishes.isEmpty()) return;

        List<Integer> ids = dishes.stream()
                .map(DishResponseDto::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;

        Map<Integer, OrderDishReviewRepository.DishRatingAgg> aggById = new HashMap<>();
        orderDishReviewRepository.aggregateRatingsForDishIds(ids)
                .forEach(a -> aggById.put(a.getDishId(), a));

        dishes.forEach(d -> {
            var agg = aggById.get(d.getId());
            if (agg == null) {
                d.setAvgRating(null);
                d.setRatingsCount(0L);
            } else {
                d.setAvgRating(agg.getAvgRating());
                d.setRatingsCount(agg.getRatingsCount());
            }
        });
    }
}

