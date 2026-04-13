package com.example.CourseWork.controller;

import com.example.CourseWork.dto.MenuResponseDto;
import com.example.CourseWork.dto.MenuDto;
import com.example.CourseWork.dto.MenuWithDishesDto;
import com.example.CourseWork.service.MenuService;
import com.example.CourseWork.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final QrCodeService qrCodeService;

    @Value("${frontend.menu.url:http://localhost:8081/menu?id=}")
    private String frontendMenuUrl;

    @GetMapping
    public ResponseEntity<List<MenuWithDishesDto>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenusWithDishes());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PostMapping
    public ResponseEntity<MenuResponseDto> createMenu(@Valid @RequestBody MenuDto dto) {
        return ResponseEntity.ok(menuService.createMenu(dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @PutMapping("/{id}")
    public ResponseEntity<MenuResponseDto> updateMenu(@PathVariable Integer id, @Valid @RequestBody MenuDto dto) {
        return ResponseEntity.ok(menuService.updateMenu(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Integer id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/qrcode")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> getMenuQrCode(@PathVariable Integer id) {
        String qrContent = frontendMenuUrl + id;
        byte[] qrImage = qrCodeService.generateQrCode(qrContent, 300, 300);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"menu-" + id + "-qr.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}
