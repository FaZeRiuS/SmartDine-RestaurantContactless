package com.example.CourseWork.model;

import lombok.Data;

@Data
public class KeycloakUser {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
} 