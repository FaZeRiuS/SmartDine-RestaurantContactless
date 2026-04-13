package com.example.CourseWork.controller;

import com.example.CourseWork.dto.dashboard.DashboardViewDto;
import com.example.CourseWork.service.DashboardService;
import com.example.CourseWork.service.QrCodeService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@SuppressWarnings("null")
class AdminControllerTest extends BaseControllerTest {

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private QrCodeService qrCodeService;

    @Test
    void dashboard_ShouldRequireAdmin() throws Exception {
        // Arrange
        com.example.CourseWork.dto.dashboard.DashboardSummaryDto summary = new com.example.CourseWork.dto.dashboard.DashboardSummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        DashboardViewDto dto = new DashboardViewDto(summary, java.util.List.of(), java.util.List.of());
        when(dashboardService.getAdminDashboard()).thenReturn(dto);

        // Act & Assert - Forbidden for Chef
        mockMvc.perform(get("/admin/dashboard")
                        .with(withUser("chef-1", "CHEF")))
                .andExpect(status().isForbidden());

        // Act & Assert - OK for Admin
        mockMvc.perform(get("/admin/dashboard")
                        .with(withUser("admin-1", "ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("summary", "topDishes"));
    }

    @Test
    void tableQr_ShouldReturnPng() throws Exception {
        // Arrange
        byte[] fakeQr = new byte[]{1, 2, 3};
        when(qrCodeService.generateQrCode(anyString(), anyInt(), anyInt())).thenReturn(fakeQr);

        // Act & Assert
        mockMvc.perform(get("/admin/qr/table")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .param("table", "5")
                        .param("size", "256"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"table-5-qr.png\""))
                .andExpect(content().bytes(fakeQr));
    }

    @Test
    void tableQr_ShouldValidateParams() throws Exception {
        // Act & Assert - Invalid table
        mockMvc.perform(get("/admin/qr/table")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .param("table", "600")) // Max is 500
                .andExpect(status().isBadRequest());

        // Act & Assert - Invalid size
        mockMvc.perform(get("/admin/qr/table")
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .param("table", "1")
                        .param("size", "50")) // Min is 120
                .andExpect(status().isBadRequest());
    }
}
