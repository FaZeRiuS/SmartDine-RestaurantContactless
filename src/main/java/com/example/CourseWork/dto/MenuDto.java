package com.example.CourseWork.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class MenuDto {
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
}

