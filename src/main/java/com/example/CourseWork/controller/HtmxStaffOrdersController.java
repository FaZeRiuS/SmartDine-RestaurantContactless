package com.example.CourseWork.controller;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/htmx/staff/orders")
@RequiredArgsConstructor
public class HtmxStaffOrdersController {

    private static final DateTimeFormatter RENDERED_AT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OrderService orderService;

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF', 'WAITER')")
    public String board(@RequestParam(defaultValue = "all") String filter, Model model) {
        populateBoardModel(filter, model);
        return "fragments/staff-orders :: board";
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CHEF', 'WAITER')")
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus newStatus,
            @RequestParam(defaultValue = "all") String filter,
            Model model) {
        orderService.updateOrderStatus(id, newStatus);
        populateBoardModel(filter, model);
        model.addAttribute("message", "Замовлення #" + id + ": статус «" + getStatusDisplayName(newStatus) + "»");
        model.addAttribute("type", "success");
        return "fragments/staff-orders :: board_with_toast";
    }

    @DeleteMapping("/{id}/call-waiter")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'WAITER')")
    public String dismissWaiter(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "all") String filter,
            Model model) {
        orderService.dismissWaiterCall(id);
        populateBoardModel(filter, model);
        model.addAttribute("message", "Виклик за замовленням #" + id + " опрацьовано");
        model.addAttribute("type", "success");
        return "fragments/staff-orders :: board_with_toast";
    }

    private void populateBoardModel(String filter, Model model) {
        List<OrderResponseDto> all = orderService.getActiveOrders();
        List<OrderResponseDto> filtered = applyFilter(all, filter);
        List<OrderResponseDto> inProgress = filtered.stream()
                .filter(o -> o.getStatus() == OrderStatus.NEW || o.getStatus() == OrderStatus.PREPARING)
                .toList();
        List<OrderResponseDto> ready = filtered.stream()
                .filter(o -> o.getStatus() == OrderStatus.READY)
                .toList();
        model.addAttribute("staffFilter", filter == null ? "all" : filter);
        model.addAttribute("ordersInProgress", inProgress);
        model.addAttribute("ordersReady", ready);
        model.addAttribute("boardEmpty", inProgress.isEmpty() && ready.isEmpty());
        model.addAttribute("boardRenderedAt", RENDERED_AT.format(LocalTime.now()));
    }

    private static List<OrderResponseDto> applyFilter(List<OrderResponseDto> all, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return all;
        }
        try {
            OrderStatus s = OrderStatus.valueOf(filter.trim().toUpperCase());
            return all.stream().filter(o -> o.getStatus() == s).toList();
        } catch (IllegalArgumentException e) {
            return all;
        }
    }

    private String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case NEW -> "Нове";
            case PREPARING -> "Готується";
            case READY -> "Готово";
            case COMPLETED -> "Завершено";
            case CANCELLED -> "Скасовано";
        };
    }
}
