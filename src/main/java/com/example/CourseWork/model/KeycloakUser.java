package com.example.CourseWork.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KeycloakUser {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
} 