package com.example.CourseWork.controller;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DishController.class)
@SuppressWarnings("null")
class DishControllerTest extends BaseControllerTest {

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void getAllAvailableDishes_ShouldBePublic() throws Exception {
        // Arrange
        DishResponseDto dish = new DishResponseDto();
        dish.setId(1);
        dish.setName("Borscht");
        when(dishService.getAllAvailableDishes()).thenReturn(List.of(dish));

        // Act & Assert
        mockMvc.perform(get("/api/dishes/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Borscht"));
    }

    @Test
    void createDish_ShouldRequireAdminOrChef() throws Exception {
        // Arrange
        DishDto dto = new DishDto();
        dto.setName("New Dish");

        // Act & Assert - Forbidden for Customer
        mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Dish\"}")
                        .with(withUser("cust-1", "CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Admin
        when(dishService.createDish(any())).thenReturn(new DishResponseDto());
        mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Dish\"}")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void getRecommendations_ShouldRequireCustomer() throws Exception {
        // Arrange
        when(recommendationService.getRecommendations("cust-1")).thenReturn(List.of());

        // Act & Assert - Forbidden for Guest (Anonymous)
        mockMvc.perform(get("/api/dishes/recommendations"))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Customer
        mockMvc.perform(get("/api/dishes/recommendations")
                        .with(withUser("cust-1", "CUSTOMER")))
                .andExpect(status().isOk());
        
        verify(recommendationService).getRecommendations("cust-1");
    }

    @Test
    void getSmartCombo_ShouldBePublic() throws Exception {
        // Arrange
        DishResponseDto combo = new DishResponseDto();
        combo.setId(2);
        combo.setName("Combo Dish");
        when(dishService.getSmartCombo(eq(1), org.mockito.ArgumentMatchers.<java.util.List<Integer>>any()))
                .thenReturn(java.util.Optional.of(combo));

        // Act & Assert
        mockMvc.perform(get("/api/dishes/1/smart-combo")
                        .param("cartDishIds", "10,20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Combo Dish"));
    }
}
