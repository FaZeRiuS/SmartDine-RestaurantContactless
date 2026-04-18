package com.example.CourseWork.controller;

import com.example.CourseWork.dto.menu.MenuDto;
import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.service.menu.MenuService;
import com.example.CourseWork.service.qr.QrCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
class MenuControllerTest extends BaseControllerTest {

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private QrCodeService qrCodeService;

    @Test
    @SuppressWarnings("null")
    void getAllMenus_ShouldReturnPublicAccess() throws Exception {
        // Arrange
        when(menuService.getAllMenusWithDishes()).thenReturn(List.of(new MenuWithDishesDto()));

        // Act & Assert
        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @SuppressWarnings("null")
    void createMenu_ShouldRequireAdminOrChef() throws Exception {
        // Arrange
        MenuResponseDto response = new MenuResponseDto();
        response.setId(1);
        response.setName("New Menu");
        when(menuService.createMenu(any(MenuDto.class))).thenReturn(response);

        // Act & Assert - Unauthorized (No user)
        mockMvc.perform(post("/api/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Menu\"}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        // Act & Assert - Authorized as ADMIN
        mockMvc.perform(post("/api/menus")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Menu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Menu"));
    }

    @Test
    @SuppressWarnings("null")
    void getMenuQrCode_ShouldReturnPngImage() throws Exception {
        // Arrange
        byte[] mockQr = new byte[]{1, 2, 3};
        when(qrCodeService.generateQrCode(anyString(), eq(300), eq(300))).thenReturn(mockQr);

        // Act & Assert
        mockMvc.perform(get("/api/menus/1/qrcode"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"menu-1-qr.png\""));
    }
}
