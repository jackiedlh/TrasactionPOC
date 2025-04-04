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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hsbc.transaction.model.Transaction.revertTransaction;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
                //transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.FAILED);
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
                transactionService.updateTransactionStatus(createdDebit.getTransactionId(), TransactionStatus.FAILED);
                transactionService.updateTransactionStatus(createdCredit.getTransactionId(), TransactionStatus.FAILED);

                Transaction revertedTransaction = revertTransaction(createdDebit);
                transactionService.createTransaction(revertedTransaction);
                transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
                revertedTransaction = revertTransaction(createdCredit);
                accountService.updateAccountBalance(revertedTransaction);
                transactionService.updateTransactionStatus(revertedTransaction.getTransactionId(), TransactionStatus.SUCCESS);
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
                        revertTxs.add(revertTransaction(createdDebit));
                        Transaction createdCredit = transactionService.createTransaction(creditTx);
                        accountService.updateAccountBalance(createdCredit);
                        revertTxs.add(revertTransaction(createdCredit));

                        transactionService.updateTransactionStatus(createdDebit.getTransactionId(), TransactionStatus.SUCCESS);
                        transactionService.updateTransactionStatus(createdCredit.getTransactionId(), TransactionStatus.SUCCESS);
                    } catch (Exception e) {
                        System.err.println("Transfer failed: " + e.getMessage());
                        for (Transaction transaction : revertTxs) {
                            transactionService.createTransaction(transaction);
                            accountService.updateAccountBalance(transaction);
                            transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
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
} 