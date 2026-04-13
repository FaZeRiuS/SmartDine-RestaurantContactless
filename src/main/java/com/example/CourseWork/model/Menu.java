package com.example.CourseWork.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @NotBlank(message = "Menu name cannot be blank")
    @Size(max = 255)
    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "start_time")
    @ToString.Include
    private LocalTime startTime;

    @Column(name = "end_time")
    @ToString.Include
    private LocalTime endTime;

    @ManyToMany(mappedBy = "menus", cascade = CascadeType.ALL)
    private List<Dish> dishes = new java.util.ArrayList<>();
}
