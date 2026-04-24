package com.example.CourseWork.service.user.impl;

import com.example.CourseWork.repository.UserAllergenExclusionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.example.CourseWork.model.UserAllergenExclusion;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserPreferenceServiceImplTest {

    @Mock
    private UserAllergenExclusionRepository repository;

    @InjectMocks
    private UserPreferenceServiceImpl service;

    @Test
    void setExcludedAllergens_ShouldReplaceAll() {
        String userId = "u1";
        service.setExcludedAllergens(userId, List.of("milk", " egg "));
        verify(repository, times(1)).deleteByUserId(userId);
        verify(repository, times(1)).saveAll(org.mockito.Mockito.<Iterable<UserAllergenExclusion>>any());
    }

    @Test
    void setExcludedAllergens_EmptyList_ShouldJustDelete() {
        String userId = "u1";
        service.setExcludedAllergens(userId, List.of());
        verify(repository, times(1)).deleteByUserId(userId);
        verify(repository, never()).saveAll(org.mockito.Mockito.<Iterable<UserAllergenExclusion>>any());
    }
}

