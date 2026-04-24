package com.example.CourseWork.controller;

import com.example.CourseWork.dto.cart.CartResponseDto;
import com.example.CourseWork.dto.menu.MenuSummaryDto;
import com.example.CourseWork.service.cart.CartService;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.menu.MenuService;
import com.example.CourseWork.service.order.OrderService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(PageController.class)
@WithMockUser
@SuppressWarnings("null")
class MenuFrontendIntegrationTest extends BaseControllerTest {


    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private Clock appClock;

    private WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
        webClient.getOptions().setJavaScriptEnabled(false);

        // PageController uses LocalTime.now(appClock); a raw mock returns nulls -> NPE -> 500.
        when(appClock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(appClock.instant()).thenReturn(Instant.parse("2026-04-15T10:00:00Z"));

        CartResponseDto emptyCart = new CartResponseDto();
        emptyCart.setItems(List.of());
        when(cartService.getCartByUserId(any())).thenReturn(emptyCart);

        when(orderService.getMyActiveOrder(any())).thenReturn(Optional.empty());
    }

    @Test
    void menuPage_ShouldRenderAvailableMenus() throws Exception {
        // Arrange
        MenuSummaryDto menu = new MenuSummaryDto();
        menu.setId(1);
        menu.setName("Lunch Special");

        when(menuService.getActiveMenusSummary()).thenReturn(List.of(menu));

        // Act
        HtmlPage page = webClient.getPage("http://localhost/menu");

        // Assert
        String content = page.asNormalizedText();
        assertThat(content).contains("Lunch Special");
        assertThat(page.getTitleText()).contains("Меню");
    }

    @Test
    void cartPage_ShouldRenderCartTitle() throws Exception {
        // Act
        HtmlPage page = webClient.getPage("http://localhost/cart");

        // Assert
        assertThat(page.asNormalizedText()).contains("Кошик");
    }
}
