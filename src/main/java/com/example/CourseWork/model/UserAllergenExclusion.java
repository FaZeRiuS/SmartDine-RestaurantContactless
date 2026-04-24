package com.example.CourseWork.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_allergen_exclusions")
public class UserAllergenExclusion {
    @EmbeddedId
    private UserAllergenExclusionId id;
}

