package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.LoyaltyTransactionType;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.InsufficientPointsException;
import com.example.CourseWork.model.LoyaltyAccount;
import com.example.CourseWork.model.LoyaltyTransaction;
import com.example.CourseWork.repository.LoyaltyAccountRepository;
import com.example.CourseWork.repository.LoyaltyTransactionRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal RATE_NEW = new BigDecimal("0.01");
    private static final BigDecimal RATE_10 = new BigDecimal("0.03");
    private static final BigDecimal RATE_100 = new BigDecimal("0.05");

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void earnPoints(UUID userId, BigDecimal orderAmount) {
        earnPointsInternal(userId, orderAmount, null);
    }

    /**
     * Variant used for external events (e.g. LiqPay callback) where we need idempotency.
     */
    @Override
    @Transactional
    public void earnPointsInternal(UUID userId, BigDecimal orderAmount, String reference) {
        if (userId == null) {
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        }
        if (orderAmount == null) {
            throw new BadRequestException(ErrorMessages.ORDER_AMOUNT_REQUIRED);
        }
        if (orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (reference != null && !reference.isBlank() && loyaltyTransactionRepository.existsByReference(reference)) {
            return;
        }

        BigDecimal rate = resolveCashbackRate(userId);
        BigDecimal points = normalizeMoney(orderAmount.multiply(rate));

        if (points.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        creditPointsInternal(userId, points, reference);
    }

    /**
     * Credits points as-is (no cashback percentage multiplication).
     * Used for adjustments/refunds.
     */
    @Override
    @Transactional
    public void creditPointsInternal(UUID userId, BigDecimal points, String reference) {
        if (userId == null) {
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        }
        if (points == null) {
            throw new BadRequestException(ErrorMessages.POINTS_REQUIRED);
        }
        BigDecimal toCredit = normalizeMoney(points);
        if (toCredit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (reference != null && !reference.isBlank() && loyaltyTransactionRepository.existsByReference(reference)) {
            return;
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    LoyaltyAccount created = new LoyaltyAccount();
                    created.setUserId(userId);
                    created.setBalance(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
                    return loyaltyAccountRepository.save(created);
                });

        BigDecimal current = normalizeMoney(account.getBalance());
        BigDecimal updated = normalizeMoney(current.add(toCredit));
        account.setBalance(updated);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setAccount(account);
        tx.setType(LoyaltyTransactionType.EARN);
        tx.setAmount(toCredit);
        tx.setBalanceAfter(updated);
        tx.setReference(reference != null && !reference.isBlank() ? reference : null);
        loyaltyTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void spendPoints(UUID userId, BigDecimal amount) {
        spendPointsInternal(userId, amount, null);
    }

    /**
     * Variant used for idempotent spends (e.g. setting order coverage amount).
     */
    @Override
    @Transactional
    public void spendPointsInternal(UUID userId, BigDecimal amount, String reference) {
        if (userId == null) {
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        }
        if (amount == null) {
            throw new BadRequestException(ErrorMessages.AMOUNT_REQUIRED);
        }

        BigDecimal toSpend = normalizeMoney(amount);
        if (toSpend.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (reference != null && !reference.isBlank() && loyaltyTransactionRepository.existsByReference(reference)) {
            return;
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    LoyaltyAccount created = new LoyaltyAccount();
                    created.setUserId(userId);
                    created.setBalance(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
                    return loyaltyAccountRepository.save(created);
                });

        BigDecimal current = normalizeMoney(account.getBalance());
        if (current.compareTo(toSpend) < 0) {
            throw new InsufficientPointsException("Недостатньо балів. Поточний баланс: " + current);
        }

        BigDecimal updated = normalizeMoney(current.subtract(toSpend));
        account.setBalance(updated);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setAccount(account);
        tx.setType(LoyaltyTransactionType.SPEND);
        tx.setAmount(toSpend);
        tx.setBalanceAfter(updated);
        tx.setReference(reference != null && !reference.isBlank() ? reference : null);
        loyaltyTransactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID userId) {
        if (userId == null) {
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        }
        return loyaltyAccountRepository.findByUserId(userId)
                .map(a -> normalizeMoney(a.getBalance()))
                .orElse(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getSuccessfulOrdersCount(UUID userId) {
        if (userId == null) {
            throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        }
        return orderRepository.countSuccessfulOrdersByUserId(userId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveCashbackRate(UUID userId) {
        Long count = getSuccessfulOrdersCount(userId);
        if (count != null && count >= 100) {
            return RATE_100;
        }
        if (count != null && count >= 10) {
            return RATE_10;
        }
        return RATE_NEW;
    }
}

