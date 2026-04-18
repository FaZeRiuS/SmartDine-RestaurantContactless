package com.example.CourseWork.controller.htmx;

import com.example.CourseWork.controller.BaseControllerTest;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderItemResponseDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HtmxCustomerOrdersController.class)
@SuppressWarnings("null")
class HtmxCustomerOrdersControllerTest extends BaseControllerTest {

    @MockitoBean
    private OrderService orderService;

    @Test
    void customerOrders_activeTab_shouldRenderOrderFragment() throws Exception {
        OrderItemResponseDto item = new OrderItemResponseDto();
        item.setDishName("Borscht");
        item.setQuantity(1);
        item.setPrice(new BigDecimal("8.00"));

        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(99);
        dto.setStatus(OrderStatus.NEW);
        dto.setPaymentStatus(PaymentStatus.PENDING);
        dto.setTotalPrice(new BigDecimal("8.00"));
        dto.setItems(List.of(item));

        when(orderService.getOrderHistoryForStatuses(
                eq("user-id"),
                any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/htmx/customer/orders").param("tab", "active")
                        .with(withUser("user-id", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Замовлення #99")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Borscht")));
    }
}
