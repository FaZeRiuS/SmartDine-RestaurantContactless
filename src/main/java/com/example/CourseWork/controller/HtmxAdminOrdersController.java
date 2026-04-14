package com.example.CourseWork.controller;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/htmx/admin/orders")
@RequiredArgsConstructor
public class HtmxAdminOrdersController {

    private final OrderService orderService;

    @GetMapping("/table")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String ordersTable(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderResponseDto> ordersPage =
                orderService.getAllOrdersFiltered(pageable, status, paymentStatus);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterPayment", paymentStatus);
        model.addAttribute("filterQuery", buildFilterQuery(status, paymentStatus));
        return "fragments/admin-orders-history :: ordersBlock";
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getOrderDetailForAdmin(id));
        return "fragments/admin-orders-history :: orderDetailBody";
    }

    private static String buildFilterQuery(OrderStatus status, PaymentStatus paymentStatus) {
        StringBuilder sb = new StringBuilder();
        if (status != null) {
            sb.append("&status=").append(status.name());
        }
        if (paymentStatus != null) {
            sb.append("&paymentStatus=").append(paymentStatus.name());
        }
        return sb.toString();
    }
}
