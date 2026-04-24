package com.example.CourseWork.service.user.impl;

import com.example.CourseWork.model.UserAllergenExclusion;
import com.example.CourseWork.model.UserAllergenExclusionId;
import com.example.CourseWork.repository.UserAllergenExclusionRepository;
import com.example.CourseWork.service.user.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserAllergenExclusionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getExcludedAllergens(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return repository.findAllAllergensByUserId(userId).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void setExcludedAllergens(String userId, Collection<String> allergens) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        repository.deleteByUserId(userId);

        if (allergens == null || allergens.isEmpty()) {
            return;
        }

        Set<String> normalized = allergens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        if (normalized.isEmpty()) {
            return;
        }

        List<UserAllergenExclusion> rows = new ArrayList<>(normalized.size());
        for (String a : normalized) {
            UserAllergenExclusionId id = new UserAllergenExclusionId();
            id.setUserId(userId);
            id.setAllergen(a);
            UserAllergenExclusion e = new UserAllergenExclusion();
            e.setId(id);
            rows.add(e);
        }
        repository.saveAll(rows);
    }
}

