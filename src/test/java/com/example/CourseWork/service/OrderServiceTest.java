package com.example.CourseWork.service;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.KeycloakUser;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.*;
import com.example.CourseWork.service.impl.OrderServiceImpl;
import com.example.CourseWork.util.KeycloakUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DishRepository dishRepository;
    @Mock private CartRepository cartRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private PaymentService paymentService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private OrderServiceReviewRepository orderServiceReviewRepository;
    @Mock private OrderDishReviewRepository orderDishReviewRepository;
    @Mock private PushNotificationService pushNotificationService;

    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private OrderServiceImpl orderService;

    private MockedStatic<KeycloakUtil> mockedKeycloakUtil;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    private static final String USER_ID = "user-123";
    private static final String STRANGER_ID = "stranger-456";

    @BeforeEach
    void setUp() {
        mockedKeycloakUtil = Mockito.mockStatic(KeycloakUtil.class);
        mockedSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class);
        
        lenient().when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        mockedKeycloakUtil.close();
        mockedSecurityContextHolder.close();
    }

    private KeycloakUser createMockUser(String id) {
        KeycloakUser user = new KeycloakUser();
        user.setId(id);
        return user;
    }

    @Test
    void getOrderById_AsOwner_ShouldSuccess() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(USER_ID);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        mockedKeycloakUtil.when(KeycloakUtil::getCurrentUser).thenReturn(createMockUser(USER_ID));
        when(orderMapper.toResponseDto(order)).thenReturn(new OrderResponseDto());

        // Act
        OrderResponseDto result = orderService.getOrderById(1);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void getOrderById_AsStaff_ShouldSuccess() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(STRANGER_ID);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        mockedKeycloakUtil.when(KeycloakUtil::getCurrentUser).thenReturn(createMockUser(USER_ID));
        
        // Mock staff role
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_WAITER"))).when(authentication).getAuthorities();

        when(orderMapper.toResponseDto(order)).thenReturn(new OrderResponseDto());

        // Act
        OrderResponseDto result = orderService.getOrderById(1);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void getOrderById_AsStranger_ShouldThrowException() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(STRANGER_ID);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        mockedKeycloakUtil.when(KeycloakUtil::getCurrentUser).thenReturn(createMockUser(USER_ID));
        
        // Mock no staff roles
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Access denied");
    }

    @Test
    void updateOrderItemQuantity_DecreaseWhenPaid_ShouldThrowException() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        com.example.CourseWork.model.OrderItem item = new com.example.CourseWork.model.OrderItem();
        item.setId(10);
        item.setQuantity(5);
        order.getItems().add(item);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderItemQuantity(1, USER_ID, 10, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot decrease quantity for an order that is already processing or paid");
    }

    @Test
    void payOrder_WhenReady_ShouldChangeToCompleted() throws Exception {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.READY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(100.0f);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(paymentService.processPayment(anyFloat(), anyString())).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        orderService.payOrder(1, USER_ID, "pm_test");

        // Assert
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void payOrder_ShouldAuthorizeOwnerOnly() {
        // Arrange
        Order order = new Order();
        order.setId(1);
        order.setUserId(STRANGER_ID);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.payOrder(1, USER_ID, "pm_test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void confirmOrderFromCart_EmptyCart_ShouldThrowException() {
        // Arrange
        com.example.CourseWork.model.Cart cart = new com.example.CourseWork.model.Cart();
        cart.setItems(Collections.emptyList());

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThatThrownBy(() -> orderService.confirmOrderFromCart(USER_ID, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cart is empty");
    }
}
