package com.example.CourseWork.controller;

import com.example.CourseWork.dto.dashboard.DashboardViewDto;
import com.example.CourseWork.service.DashboardService;
import com.example.CourseWork.service.QrCodeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {

    private final DashboardService dashboardService;
    private final QrCodeService qrCodeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) throws Exception {
        DashboardViewDto dto = dashboardService.getAdminDashboard();
        model.addAttribute("summary", dto.summary());
        model.addAttribute("topDishes", dto.topDishes());
        model.addAttribute("hourlyOrdersToday", dto.hourlyOrdersToday());

        return "admin/dashboard";
    }

    @GetMapping("/qr")
    public String qrRedirectToDashboard() {
        return "redirect:/admin/dashboard#qr";
    }

    @GetMapping(value = "/qr/table", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> tableQr(
            @RequestParam("table") int table,
            @RequestParam(value = "size", defaultValue = "320") int size,
            HttpServletRequest request
    ) {
        if (table <= 0 || table > 500) {
            return ResponseEntity.badRequest().build();
        }
        if (size < 120 || size > 1024) {
            return ResponseEntity.badRequest().build();
        }

        String targetUrl = ServletUriComponentsBuilder.fromContextPath(request)
                .path("/")
                .queryParam("table", table)
                .build()
                .toUriString();

        byte[] png = qrCodeService.generateQrCode(targetUrl, size, size);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"table-" + table + "-qr.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}

