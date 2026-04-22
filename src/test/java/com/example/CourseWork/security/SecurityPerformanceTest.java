package com.example.CourseWork.security;

import com.example.CourseWork.controller.BaseControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.CourseWork.service.order.OrderService;
import org.springframework.data.domain.Page;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityPerformanceTest extends BaseControllerTest {

    @MockitoBean
    private OrderService orderService;

    @Test
    void testAuthorizationTimeIsUnder200ms() throws Exception {
        // Mock the service to avoid database issues
        when(orderService.getOrderHistory(any(), any())).thenReturn(Page.empty());
        MvcResult result = mockMvc.perform(get("/api/orders/history")
                        .with(withUser("test-user", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Security-Time-Ms"))
                .andReturn();

        String timeStr = result.getResponse().getHeader("X-Security-Time-Ms");
        long timeMs = Long.parseLong(timeStr);

        System.out.println("Authorization time measured in test: " + timeMs + " ms");
        
        // Assert that it's under 200ms
        assertTrue(timeMs < 200, "Authorization time " + timeMs + "ms exceeds 200ms limit");
    }
}
