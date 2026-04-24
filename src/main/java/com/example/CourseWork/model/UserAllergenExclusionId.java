package com.example.CourseWork.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class UserAllergenExclusionId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "allergen", nullable = false, length = 64)
    private String allergen;
}

