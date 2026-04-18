package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderTipService;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OrderTipServiceImpl implements OrderTipService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal TIP_MAX = new BigDecimal("10000.00");

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderPaymentPolicy orderPaymentPolicy;

    @Override
    @Transactional
    public OrderResponseDto setTip(Integer orderId, String userId, BigDecimal amount) {
        if (orderId == null)
            throw new BadRequestException(ErrorMessages.ORDER_ID_REQUIRED);
        if (userId == null)
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderPaymentPolicy.assertOwner(order, userId);
        orderPaymentPolicy.assertNotPaid(order);

        BigDecimal tip = normalizeMoney(amount);
        if (tip.compareTo(BigDecimal.ZERO) < 0)
            tip = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (tip.compareTo(TIP_MAX) > 0)
            tip = TIP_MAX;

        order.setTipAmount(tip);
        Order saved = orderRepository.save(order);
        return orderMapper.toResponseDto(saved);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
