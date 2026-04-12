package com.example.CourseWork.controller;

import com.example.CourseWork.util.KeycloakUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUser() {
        return ResponseEntity.ok(Map.of("id", KeycloakUtil.getCurrentUser().getId()));
    }
}
