package com.example.CourseWork.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
@Entity
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

    private LocalTime startTime;
    private LocalTime endTime;

    @ManyToMany(mappedBy = "menus", cascade = CascadeType.ALL)
    private List<Dish> dishes = new java.util.ArrayList<>();
}

