package com.example.CourseWork.service;

import com.example.CourseWork.exception.InsufficientPointsException;
import com.example.CourseWork.model.LoyaltyAccount;
import com.example.CourseWork.repository.LoyaltyAccountRepository;
import com.example.CourseWork.repository.LoyaltyTransactionRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.impl.LoyaltyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LoyaltyServiceTest {

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private LoyaltyServiceImpl loyaltyService;

    @NonNull
    private UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void resolveCashbackRate_ShouldReturn1PercentForNewUsers() {
        when(orderRepository.countSuccessfulOrdersByUserId(userId.toString())).thenReturn(0L);
        BigDecimal rate = loyaltyService.resolveCashbackRate(userId);
        assertThat(rate).isEqualByComparingTo("0.01");
    }

    @Test
    void resolveCashbackRate_ShouldReturn3PercentFor10Orders() {
        when(orderRepository.countSuccessfulOrdersByUserId(userId.toString())).thenReturn(10L);
        BigDecimal rate = loyaltyService.resolveCashbackRate(userId);
        assertThat(rate).isEqualByComparingTo("0.03");
    }

    @Test
    void resolveCashbackRate_ShouldReturn5PercentFor100Orders() {
        when(orderRepository.countSuccessfulOrdersByUserId(userId.toString())).thenReturn(100L);
        BigDecimal rate = loyaltyService.resolveCashbackRate(userId);
        assertThat(rate).isEqualByComparingTo("0.05");
    }

    @Test
    void earnPointsInternal_ShouldNotEarnPointsIfReferenceExists() {
        String reference = "ref_123";
        when(loyaltyTransactionRepository.existsByReference(reference)).thenReturn(true);

        loyaltyService.earnPointsInternal(userId, new BigDecimal("100.00"), reference);

        verify(loyaltyAccountRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    void earnPointsInternal_ShouldCreateNewAccountAndCreditPoints() {
        UUID newUserId = UUID.randomUUID();
        when(loyaltyTransactionRepository.existsByReference(any())).thenReturn(false);
        when(orderRepository.countSuccessfulOrdersByUserId(newUserId.toString())).thenReturn(0L); // 1% rate
        when(loyaltyAccountRepository.findByUserIdForUpdate(newUserId)).thenReturn(Optional.empty());

        // We expect any saved account to be returned by save for the service to continue
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenAnswer(i -> i.getArguments()[0]);

        loyaltyService.earnPointsInternal(newUserId, new BigDecimal("100.00"), "new_ref");

        verify(loyaltyAccountRepository, times(2)).save(argThat(account -> 
            account.getUserId().equals(newUserId) && 
            (account.getBalance().compareTo(BigDecimal.ZERO) == 0 || account.getBalance().compareTo(new BigDecimal("1.00")) == 0)
        ));
        verify(loyaltyTransactionRepository).save(any());
    }

    @Test
    void spendPointsInternal_ShouldDeductPointsIfBalanceSufficient() {
        LoyaltyAccount account = new LoyaltyAccount();
        account.setUserId(userId);
        account.setBalance(new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP));

        when(loyaltyAccountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(loyaltyTransactionRepository.existsByReference(any())).thenReturn(false);

        loyaltyService.spendPointsInternal(userId, new BigDecimal("30.00"), "spend_ref");

        assertThat(account.getBalance()).isEqualByComparingTo("20.00");
        verify(loyaltyAccountRepository).save(account);
        verify(loyaltyTransactionRepository).save(any());
    }

    @Test
    void spendPointsInternal_ShouldThrowExceptionIfBalanceInsufficient() {
        LoyaltyAccount account = new LoyaltyAccount();
        account.setUserId(userId);
        account.setBalance(new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP));

        when(loyaltyAccountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(loyaltyTransactionRepository.existsByReference(any())).thenReturn(false);

        assertThatThrownBy(() -> loyaltyService.spendPointsInternal(userId, new BigDecimal("15.00"), "too_much"))
                .isInstanceOf(InsufficientPointsException.class)
                .hasMessageContaining("Недостатньо балів");

        verify(loyaltyAccountRepository, never()).save(any());
    }
}
