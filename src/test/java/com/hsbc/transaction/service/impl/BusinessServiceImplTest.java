package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.BusinessService;
import com.hsbc.transaction.service.TransactionService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BusinessServiceImplTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Create test accounts with initial balances
        try {
            accountService.deleteAccount("ACC001");
        }catch (Exception e){
            //ignore
        }
        try {
            accountService.deleteAccount("ACC002");
        }catch (Exception e){
            //ignore
        }
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));
        accountService.createAccount("ACC002", new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Combine Transactions Tests")
    class CombineTransactionsTests {

        @Test
        @DisplayName("Should successfully process multiple transactions")
        @Transactional
        void shouldProcessMultipleTransactions() {
            // Arrange
            List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.CREDIT)
                    .build()
            );

            // Act
            businessService.combine(transactions);

            // Assert
            assertEquals(new BigDecimal("900.00"), accountService.getBalance("ACC001"));
            assertEquals(new BigDecimal("600.00"), accountService.getBalance("ACC002"));
        }

        @Test
        @DisplayName("Should refund all successful transactions when one fails")
        @Transactional
        void shouldRefundSuccessfulTransactionsWhenOneFails() {
            // Arrange
            BigDecimal initialBalance1 = accountService.getBalance("ACC001");
            BigDecimal initialBalance2 = accountService.getBalance("ACC002");

            List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("1000.00")) // More than available balance
                    .direction(TransactionDirection.DEBIT)
                    .build()
            );

            // Act
            assertThrows(InsufficientBalanceException.class, () ->
                businessService.combine(transactions));

            // Verify balances are restored
            assertEquals(initialBalance1, accountService.getBalance("ACC001"));
            assertEquals(initialBalance2, accountService.getBalance("ACC002"));

            // Verify first transaction is refunded
            Transaction firstTransaction = transactionService.getTransactionOrThrow(transactions.get(0).getTransactionId());
            assertEquals(TransactionStatus.REFUNDED, firstTransaction.getStatus());

            // Verify refund transaction exists and is successful
            assertTrue(transactionService.queryTransactions(null, 0, 10).getContent()
                .stream()
                .anyMatch(t -> t.getAccountNo().equals("ACC001") 
                    && t.getAmount().equals(new BigDecimal("100.00"))
                    && t.getDirection() == TransactionDirection.CREDIT
                    && t.getStatus() == TransactionStatus.SUCCESS));
        }

        @Test
        @DisplayName("Should handle concurrent transaction combinations")
        void shouldHandleConcurrentTransactions() throws InterruptedException {
            // Arrange
            int numberOfThreads = 5;
            Thread[] threads = new Thread[numberOfThreads];
            BigDecimal amount = new BigDecimal("10.00");

            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        List<Transaction> transactions = Arrays.asList(
                                Transaction.builder()
                                        .accountNo("ACC001")
                                        .amount(amount)
                                        .direction(TransactionDirection.DEBIT)
                                        .build(),
                                Transaction.builder()
                                        .accountNo("ACC002")
                                        .amount(amount)
                                        .direction(TransactionDirection.CREDIT)
                                        .build()
                        );
                        businessService.combine(transactions);
                    } catch (Exception e) {
                        // Log exception but continue
                        System.err.println("Transaction failed: " + e.getMessage());
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Assert
            assertEquals(new BigDecimal("950.00"), accountService.getBalance("ACC001")); // 1000 - (10 * 5)
            assertEquals(new BigDecimal("550.00"), accountService.getBalance("ACC002")); // 500 + (10 * 5)
        }

        @Test
        @DisplayName("Should handle large number of transactions")
        @Transactional
        void shouldHandleLargeNumberOfTransactions() {
            // Arrange
            List<Transaction> transactions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                transactions.add(Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("1.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build());
            }

            // Act
            businessService.combine(transactions);

            // Assert
            assertEquals(new BigDecimal("900.00"), accountService.getBalance("ACC001")); // 1000 - 100
        }

        @Test
        @DisplayName("Should handle complex transaction patterns")
        @Transactional
        void shouldHandleComplexTransactionPatterns() {
            // Arrange
            List<Transaction> transactions = Arrays.asList(
                // Debit from ACC001
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                // Credit to ACC002
                Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.CREDIT)
                    .build(),
                // Debit from ACC002
                Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                // Credit to ACC001
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("50.00"))
                    .direction(TransactionDirection.CREDIT)
                    .build()
            );

            // Act
            businessService.combine(transactions);

            // Assert
            assertEquals(new BigDecimal("850.00"), accountService.getBalance("ACC001")); // 1000 - 200 + 50
            assertEquals(new BigDecimal("600.00"), accountService.getBalance("ACC002")); // 500 + 200 - 100
        }

        @Test
        @DisplayName("Should handle empty transaction list")
        @Transactional
        void shouldHandleEmptyTransactionList() {
            // Arrange
            BigDecimal initialBalance1 = accountService.getBalance("ACC001");
            BigDecimal initialBalance2 = accountService.getBalance("ACC002");
            List<Transaction> transactions = new ArrayList<>();

            // Act
            businessService.combine(transactions);

            // Assert
            assertEquals(initialBalance1, accountService.getBalance("ACC001"));
            assertEquals(initialBalance2, accountService.getBalance("ACC002"));
        }

        @Test
        @DisplayName("Should handle null transaction list")
        void shouldHandleNullTransactionList() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> businessService.combine(null));
        }
    }
} 