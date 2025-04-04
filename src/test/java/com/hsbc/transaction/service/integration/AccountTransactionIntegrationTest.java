package com.hsbc.transaction.service.integration;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hsbc.transaction.model.Transaction.revertTransaction;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountTransactionIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Initialize test accounts with initial balances
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
    @DisplayName("Single Account Transaction Tests")
    class SingleAccountTransactionTests {

        @Test
        @DisplayName("Should handle successful transaction and account update")
        void shouldHandleSuccessfulTransaction() {
            // Arrange
            Transaction transaction = Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build();

            // Act
            Transaction created = transactionService.createTransaction(transaction);
            accountService.updateAccountBalance(created);
            transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);

            // Assert
            assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
            assertEquals(TransactionStatus.SUCCESS, 
                transactionService.getTransactionOrThrow(created.getTransactionId()).getStatus());
        }

        @Test
        @DisplayName("Should rollback account balance when transaction fails")
        void shouldRollbackWhenTransactionFails() {
            // Arrange
            BigDecimal initialBalance = accountService.getBalance("ACC001");
            Transaction transaction = Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build();

            // Act
            Transaction created = transactionService.createTransaction(transaction);
            accountService.updateAccountBalance(created);
            try {
                transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                // Simulate failure
                throw new RuntimeException("Simulated failure");
            } catch (Exception e) {
                transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.REFUNDED);
                // Revert the transaction
                Transaction revertedTransaction = revertTransaction(created);
                transactionService.createTransaction(revertedTransaction);
                accountService.updateAccountBalance(revertedTransaction);
                transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
            }

            // Assert
            assertEquals(initialBalance, accountService.getBalance("ACC001"));
        }

        @Test
        @DisplayName("Should handle concurrent transactions on same account")
        void shouldHandleConcurrentTransactions() throws InterruptedException {
            // Arrange
            int numberOfThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            BigDecimal amount = new BigDecimal("10.00");

            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        Transaction transaction = Transaction.builder()
                                .accountNo("ACC001")
                                .amount(amount)
                                .direction(TransactionDirection.DEBIT)
                                .build();
                        Transaction created = transactionService.createTransaction(transaction);
                        accountService.updateAccountBalance(created);
                        transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertEquals(new BigDecimal("900.00"), accountService.getBalance("ACC001")); // 1000 - (10 * 10)
        }
    }

    @Nested
    @DisplayName("Multiple Account Transaction Tests")
    class MultipleAccountTransactionTests {

        @Test
        @DisplayName("Should handle transfer between accounts")
        @Transactional
        void shouldHandleTransferBetweenAccounts() {
            // Arrange
            Transaction debitTx = Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build();
            
            Transaction creditTx = Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.CREDIT)
                    .build();
            Transaction createdDebit = transactionService.createTransaction(debitTx);
            Transaction createdCredit = transactionService.createTransaction(creditTx);

            // Act
            try {
                // Execute both transactions

                accountService.updateAccountBalance(createdDebit);
                accountService.updateAccountBalance(createdCredit);

                transactionService.updateTransactionStatus(createdDebit.getTransactionId(), TransactionStatus.SUCCESS);
                transactionService.updateTransactionStatus(createdCredit.getTransactionId(), TransactionStatus.SUCCESS);

                // Assert
                assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
                assertEquals(new BigDecimal("700.00"), accountService.getBalance("ACC002"));
            } catch (Exception e) {
                // Rollback both transactions if either fails
                if (createdDebit.getStatus() == TransactionStatus.SUCCESS) {
                    transactionService.updateTransactionStatus(createdDebit.getTransactionId(), TransactionStatus.REFUNDED);
                    Transaction revertedTransaction = revertTransaction(createdDebit);
                    transactionService.createTransaction(revertedTransaction);
                    transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
                }

                if (createdCredit.getStatus() == TransactionStatus.SUCCESS) {
                    transactionService.updateTransactionStatus(createdCredit.getTransactionId(), TransactionStatus.REFUNDED);
                    Transaction revertedTransaction = revertTransaction(createdCredit);
                    accountService.updateAccountBalance(revertedTransaction);
                    transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
                }
                fail("Transfer should succeed: " + e.getMessage());

            }
        }

        @Test
        @DisplayName("Should handle concurrent transfers between multiple accounts")
        void shouldHandleConcurrentTransfers() throws InterruptedException {
            // Arrange
            int numberOfTransfers = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfTransfers * 2);
            CountDownLatch latch = new CountDownLatch(numberOfTransfers);
            BigDecimal transferAmount = new BigDecimal("50.00");

            // Act
            for (int i = 0; i < numberOfTransfers; i++) {
                executor.submit(() -> {
                    Transaction debitTx = Transaction.builder()
                            .accountNo("ACC001")
                            .amount(transferAmount)
                            .direction(TransactionDirection.DEBIT)
                            .build();

                    Transaction creditTx = Transaction.builder()
                            .accountNo("ACC002")
                            .amount(transferAmount)
                            .direction(TransactionDirection.CREDIT)
                            .build();
                    List<Transaction> revertTxs = new ArrayList<>();
                    try {
                        // Create and execute transfer transactions
                        Transaction createdDebit = transactionService.createTransaction(debitTx);
                        accountService.updateAccountBalance(createdDebit);
                        transactionService.updateTransactionStatus(createdDebit.getTransactionId(), TransactionStatus.SUCCESS);
                        revertTxs.add(createdDebit);
                        Transaction createdCredit = transactionService.createTransaction(creditTx);
                        accountService.updateAccountBalance(createdCredit);
                        transactionService.updateTransactionStatus(createdCredit.getTransactionId(), TransactionStatus.SUCCESS);
                        revertTxs.add(createdCredit);
                    } catch (Exception e) {
                        System.err.println("Transfer failed: " + e.getMessage());
                        for (Transaction transaction : revertTxs) {
                            transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
                            Transaction revertedTransaction = revertTransaction(transaction);
                            transactionService.createTransaction(revertedTransaction);
                            accountService.updateAccountBalance(revertedTransaction);
                            transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
                        }

                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertEquals(new BigDecimal("750.00"), accountService.getBalance("ACC001")); // 1000 - (50 * 5)
            assertEquals(new BigDecimal("750.00"), accountService.getBalance("ACC002")); // 500 + (50 * 5)
        }

        @Test
        @DisplayName("Should handle multiple transactions with insufficient funds")
        void shouldHandleInsufficientFunds() throws InterruptedException {
            // Arrange
            int numberOfThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            BigDecimal largeAmount = new BigDecimal("300.00");
            List<Exception> exceptions = new ArrayList<>();

            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    Transaction transaction = Transaction.builder()
                            .accountNo("ACC002") // Account with 500 balance
                            .amount(largeAmount)
                            .direction(TransactionDirection.DEBIT)
                            .build();
                    Transaction created = transactionService.createTransaction(transaction);
                    try {
                        accountService.updateAccountBalance(created);
                        transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                    } catch (InsufficientBalanceException e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                        transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.FAILED);
                        // Revert the transaction
                        Transaction revertedTransaction = revertTransaction(created);
                        transactionService.createTransaction(revertedTransaction);
                        accountService.updateAccountBalance(revertedTransaction);
                        transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);

                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertTrue(exceptions.size() > 0, "Should have some failed transactions");
            assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"), 
                "Balance should remain unchanged after failed transactions");
        }
    }

    @Nested
    @DisplayName("Refund Transaction Tests")
    class RefundTransactionTests {

        @Test
        @DisplayName("Should handle single transaction refund")
        void shouldHandleSingleTransactionRefund() {
            // Arrange
            BigDecimal initialBalance = accountService.getBalance("ACC001");
            Transaction transaction = Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .description("Test transaction")
                    .direction(TransactionDirection.DEBIT)
                    .build();

            // Act
            Transaction created = transactionService.createTransaction(transaction);
            accountService.updateAccountBalance(created);
            transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);

            // Verify initial transaction
            assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));

            // Refund process
            transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.REFUNDED);
            Transaction refundTx = Transaction.revertTransaction(created);
            transactionService.createTransaction(refundTx);
            accountService.updateAccountBalance(refundTx);
            transactionService.updateTransactionStatus(refundTx.getTransactionId(), TransactionStatus.SUCCESS);

            // Assert
            assertEquals(initialBalance, accountService.getBalance("ACC001"));
            assertEquals(TransactionStatus.REFUNDED, transactionService.getTransactionOrThrow(created.getTransactionId()).getStatus());
            assertEquals(TransactionStatus.SUCCESS, transactionService.getTransactionOrThrow(refundTx.getTransactionId()).getStatus());
        }

        @Test
        @DisplayName("Should handle complex multi-account transaction refunds")
        void shouldHandleComplexMultiAccountRefunds() {
            // Arrange
            BigDecimal acc1Initial = accountService.getBalance("ACC001");
            BigDecimal acc2Initial = accountService.getBalance("ACC002");
            
            List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .description("Debit from ACC001")
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                Transaction.builder()
                    .accountNo("ACC002")
                    .amount(new BigDecimal("200.00"))
                    .description("Credit to ACC002")
                    .direction(TransactionDirection.CREDIT)
                    .build()
            );

            List<Transaction> createdTransactions = new ArrayList<>();
            List<Transaction> refundTransactions = new ArrayList<>();

            try {
                // Act - Process transactions
                for (Transaction tx : transactions) {
                    Transaction created = transactionService.createTransaction(tx);
                    accountService.updateAccountBalance(created);
                    transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                    createdTransactions.add(created);
                }

                // Simulate failure after successful transactions
                throw new RuntimeException("Simulated failure after successful transactions");

            } catch (Exception e) {
                // Refund process - reverse order
                for (int i = createdTransactions.size() - 1; i >= 0; i--) {
                    Transaction tx = createdTransactions.get(i);
                    transactionService.updateTransactionStatus(tx.getTransactionId(), TransactionStatus.REFUNDED);
                    
                    Transaction refundTx = Transaction.revertTransaction(tx);
                    transactionService.createTransaction(refundTx);
                    accountService.updateAccountBalance(refundTx);
                    transactionService.updateTransactionStatus(refundTx.getTransactionId(), TransactionStatus.SUCCESS);
                    refundTransactions.add(refundTx);
                }
            }

            // Assert
            assertEquals(acc1Initial, accountService.getBalance("ACC001"));
            assertEquals(acc2Initial, accountService.getBalance("ACC002"));
            
            // Verify all original transactions are refunded
            createdTransactions.forEach(tx -> 
                assertEquals(TransactionStatus.REFUNDED, 
                    transactionService.getTransactionOrThrow(tx.getTransactionId()).getStatus()));
            
            // Verify all refund transactions are successful
            refundTransactions.forEach(tx -> 
                assertEquals(TransactionStatus.SUCCESS, 
                    transactionService.getTransactionOrThrow(tx.getTransactionId()).getStatus()));
        }

        @Test
        @DisplayName("Should handle concurrent refunds")
        void shouldHandleConcurrentRefunds() throws InterruptedException {
            // Arrange
            int numberOfThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            BigDecimal amount = new BigDecimal("50.00");
            List<Transaction> successfulTransactions = Collections.synchronizedList(new ArrayList<>());

            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        // Create and process transaction
                        Transaction tx = Transaction.builder()
                                .accountNo("ACC001")
                                .amount(amount)
                                .description("Concurrent transaction")
                                .direction(TransactionDirection.DEBIT)
                                .build();
                        
                        Transaction created = transactionService.createTransaction(tx);
                        accountService.updateAccountBalance(created);
                        transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                        successfulTransactions.add(created);

                        // Simulate random failure
                        if (Math.random() < 0.5) {
                            throw new RuntimeException("Random failure");
                        }

                    } catch (Exception e) {
                        // Refund process
                        successfulTransactions.forEach(successTx -> {
                            try {
                                transactionService.updateTransactionStatus(successTx.getTransactionId(), TransactionStatus.REFUNDED);
                                Transaction refundTx = Transaction.revertTransaction(successTx);
                                transactionService.createTransaction(refundTx);
                                accountService.updateAccountBalance(refundTx);
                                transactionService.updateTransactionStatus(refundTx.getTransactionId(), TransactionStatus.SUCCESS);
                            } catch (Exception refundEx) {
                                System.err.println("Error during refund:"+ refundEx.getMessage());
                            }
                        });
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            BigDecimal finalBalance = accountService.getBalance("ACC001");
            assertTrue(finalBalance.compareTo(new BigDecimal("1000.00")) <= 0, 
                "Final balance should not exceed initial balance");
        }

        @Test
        @DisplayName("Should handle partial refunds in transaction batch")
        void shouldHandlePartialRefundsInBatch() {
            // Arrange
            List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("100.00"))
                    .description("Transaction 1")
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("200.00"))
                    .description("Transaction 2")
                    .direction(TransactionDirection.DEBIT)
                    .build(),
                Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("2000.00")) // Will fail due to insufficient funds
                    .description("Transaction 3")
                    .direction(TransactionDirection.DEBIT)
                    .build()
            );

            BigDecimal initialBalance = accountService.getBalance("ACC001");
            List<Transaction> successfulTransactions = new ArrayList<>();

            // Act
            try {
                for (Transaction tx : transactions) {
                    Transaction created = transactionService.createTransaction(tx);
                    accountService.updateAccountBalance(created);
                    transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
                    successfulTransactions.add(created);
                }
            } catch (Exception e) {
                // Refund successful transactions
                for (Transaction tx : successfulTransactions) {
                    transactionService.updateTransactionStatus(tx.getTransactionId(), TransactionStatus.REFUNDED);
                    Transaction refundTx = Transaction.revertTransaction(tx);
                    transactionService.createTransaction(refundTx);
                    accountService.updateAccountBalance(refundTx);
                    transactionService.updateTransactionStatus(refundTx.getTransactionId(), TransactionStatus.SUCCESS);
                }
            }

            // Assert
            assertEquals(initialBalance, accountService.getBalance("ACC001"));
            successfulTransactions.forEach(tx -> 
                assertEquals(TransactionStatus.REFUNDED, 
                    transactionService.getTransactionOrThrow(tx.getTransactionId()).getStatus()));
        }
    }
} 