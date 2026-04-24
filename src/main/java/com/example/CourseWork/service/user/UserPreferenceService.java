package com.example.CourseWork.service.user;

import java.util.Collection;
import java.util.Set;

public interface UserPreferenceService {
    Set<String> getExcludedAllergens(String userId);
    void setExcludedAllergens(String userId, Collection<String> allergens);
}

