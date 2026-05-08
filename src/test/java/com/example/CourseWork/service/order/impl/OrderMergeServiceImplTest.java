package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderMergeServiceImplTest {

    @Mock private OrderRepository orderRepository;
    // OrderServiceImpl delegates to these; we don't need them to test mergeOrders
    @Mock private com.example.CourseWork.service.order.OrderCreateService orderCreateService;
    @Mock private com.example.CourseWork.service.order.OrderReadService orderReadService;
    @Mock private com.example.CourseWork.service.order.OrderItemService orderItemService;
    @Mock private com.example.CourseWork.service.order.OrderPaymentService orderPaymentService;
    @Mock private com.example.CourseWork.service.order.OrderWaiterService orderWaiterService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final String GUEST_ID = "GUEST_session123";
    private static final String AUTH_ID  = "keycloak-user-uuid";

    // ── mergeOrders ──────────────────────────────────────────────

    @Test
    void mergeOrders_whenGuestHasOrders_shouldReassignToAuthUser() {
        // Arrange
        Order o1 = new Order();
        o1.setUserId(GUEST_ID);
        o1.setStatus(OrderStatus.NEW);
        o1.setPaymentStatus(PaymentStatus.PENDING);

        Order o2 = new Order();
        o2.setUserId(GUEST_ID);
        o2.setStatus(OrderStatus.COMPLETED);
        o2.setPaymentStatus(PaymentStatus.SUCCESS);

        when(orderRepository.findAllWithItemsAndDishesByUserIdOrderByCreatedAtDesc(GUEST_ID))
                .thenReturn(List.of(o1, o2));

        // Act
        orderService.mergeOrders(GUEST_ID, AUTH_ID);

        // Assert: both orders are re-saved with the auth user's ID
        assertThat(o1.getUserId()).isEqualTo(AUTH_ID);
        assertThat(o2.getUserId()).isEqualTo(AUTH_ID);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void mergeOrders_whenGuestHasNoOrders_shouldDoNothing() {
        // Arrange
        when(orderRepository.findAllWithItemsAndDishesByUserIdOrderByCreatedAtDesc(GUEST_ID))
                .thenReturn(new ArrayList<>());

        // Act
        orderService.mergeOrders(GUEST_ID, AUTH_ID);

        // Assert: nothing is saved
        verify(orderRepository, never()).save(any());
    }

    @Test
    void mergeOrders_whenNullGuestId_shouldDoNothing() {
        orderService.mergeOrders(null, AUTH_ID);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void mergeOrders_whenSameId_shouldDoNothing() {
        orderService.mergeOrders(AUTH_ID, AUTH_ID);
        verifyNoInteractions(orderRepository);
    }
}
