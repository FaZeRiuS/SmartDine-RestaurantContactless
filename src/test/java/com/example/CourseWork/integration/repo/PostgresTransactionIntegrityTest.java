package com.example.CourseWork.integration.repo;

import com.example.CourseWork.model.LoyaltyAccount;
import com.example.CourseWork.repository.LoyaltyAccountRepository;
import com.example.CourseWork.repository.LoyaltyTransactionRepository;
import com.example.CourseWork.service.loyalty.LoyaltyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verification Suite for PostgreSQL Transaction Integrity.
 * Proves ACID compliance (Atomicity and Isolation) under concurrent load.
 */
@SpringBootTest
@ActiveProfiles("test")
public class PostgresTransactionIntegrityTest {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Test Case 1: Concurrency and Isolation (High Reliability Verification)
     * Simulates multiple concurrent updates to the same loyalty account.
     * Uses Pessimistic Locking (@Lock(LockModeType.PESSIMISTIC_WRITE)) in the repository.
     */
    @Test
    public void testConcurrencyIntegrity() {
        UUID userId = UUID.randomUUID();

        // MANUALLY create the account in a separate transaction to ensure it's committed before threads start
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            LoyaltyAccount account = new LoyaltyAccount();
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            loyaltyAccountRepository.save(account);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }

        BigDecimal increment = new BigDecimal("10.00");
        int threadCount = 20;
        int iterationsPerThread = 10;
        BigDecimal expectedBalance = increment.multiply(new BigDecimal(threadCount * iterationsPerThread))
                .setScale(2, RoundingMode.HALF_UP);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CompletableFuture<?>[] futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        loyaltyService.creditPointsInternal(userId, increment, null);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        BigDecimal finalBalance = loyaltyService.getBalance(userId);

        assertEquals(0, expectedBalance.compareTo(finalBalance),
                "Data corruption detected! Balance should be exactly " + expectedBalance + " but was " + finalBalance);
    }

    /**
     * Test Case 2: Atomicity (Rollback on Failure)
     * Verifies that if a transaction fails halfway, no partial data is persisted.
     */
    @Test
    @SuppressWarnings("null")
    public void testAtomicityRollback() {
        UUID userId = UUID.randomUUID();
        BigDecimal initialBalance = loyaltyService.getBalance(userId); // Should be 0

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            loyaltyService.creditPointsInternal(userId, new BigDecimal("100.00"), "tx-rollback-test");
            if (userId != null) {
                throw new RuntimeException("Simulated failure after partial update");
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
        }

        BigDecimal finalBalance = loyaltyService.getBalance(userId);

        assertEquals(0, initialBalance.compareTo(finalBalance),
                "Atomicity violated! Partial data was persisted despite failure.");

        boolean transactionExists = loyaltyTransactionRepository.existsByReference("tx-rollback-test");
        assertTrue(!transactionExists, "Transaction record should have been rolled back.");
    }
}

