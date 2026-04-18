package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.security.CurrentUserIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/htmx/customer/orders")
@RequiredArgsConstructor
public class HtmxCustomerOrdersController {

    private static final List<OrderStatus> ACTIVE_TAB_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
    private static final List<OrderStatus> PAST_TAB_STATUSES =
            List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);

    private final OrderService orderService;
    private final CurrentUserIdentity currentUserIdentity;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerOrders(
            @RequestParam(defaultValue = "active") String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        String userId = currentUserIdentity.currentUserId();
        List<OrderStatus> statuses =
                "past".equalsIgnoreCase(tab) ? PAST_TAB_STATUSES : ACTIVE_TAB_STATUSES;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderResponseDto> ordersPage =
                orderService.getOrderHistoryForStatuses(userId, statuses, pageable);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("tab", "past".equalsIgnoreCase(tab) ? "past" : "active");
        model.addAttribute("tabQuery", "past".equalsIgnoreCase(tab) ? "past" : "active");
        return "fragments/customer-orders :: ordersList";
    }
}
