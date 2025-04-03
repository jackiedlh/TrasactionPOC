package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.PageResponse;
import com.hsbc.transaction.model.TransactionFilter;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.exception.InvalidTransactionStateException;
import com.hsbc.transaction.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceImplTest {
    private TransactionServiceImpl transactionService;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();
        
        testTransaction = Transaction.builder()
                .accountNo("123456")
                .amount(new BigDecimal("100.00"))
                .direction(TransactionDirection.OUT)
                .build();
    }

    @Nested
    @DisplayName("Transaction Creation Tests")
    class TransactionCreationTests {
        @Test
        @DisplayName("Should create transaction with generated ID")
        void shouldCreateTransactionWithGeneratedId() {
            Transaction created = transactionService.createTransaction(testTransaction);
            
            assertNotNull(created.getTransactionId());
            assertNotNull(created.getTimestamp());
            assertEquals(TransactionStatus.RUNNING, created.getStatus());
            assertEquals(testTransaction.getAccountNo(), created.getAccountNo());
            assertEquals(testTransaction.getAmount(), created.getAmount());
            assertEquals(testTransaction.getDirection(), created.getDirection());
        }

        @Test
        @DisplayName("Should create transaction with provided ID")
        void shouldCreateTransactionWithProvidedId() {
            String transactionId = "test-id";
            testTransaction.setTransactionId(transactionId);
            
            Transaction created = transactionService.createTransaction(testTransaction);
            
            assertEquals(transactionId, created.getTransactionId());
        }

        @Test
        @DisplayName("Should throw exception when transaction is null")
        void shouldThrowExceptionWhenTransactionIsNull() {
            assertThrows(InvalidTransactionException.class, () -> 
                transactionService.createTransaction(null));
        }

        @Test
        @DisplayName("Should throw exception when account number is missing")
        void shouldThrowExceptionWhenAccountNumberIsMissing() {
            testTransaction.setAccountNo(null);
            assertThrows(InvalidTransactionException.class, () -> 
                transactionService.createTransaction(testTransaction));
        }

        @Test
        @DisplayName("Should throw exception when amount is invalid")
        void shouldThrowExceptionWhenAmountIsInvalid() {
            testTransaction.setAmount(BigDecimal.ZERO);
            assertThrows(InvalidTransactionException.class, () -> 
                transactionService.createTransaction(testTransaction));
        }

        @Test
        @DisplayName("Should throw exception when direction is missing")
        void shouldThrowExceptionWhenDirectionIsMissing() {
            testTransaction.setDirection(null);
            assertThrows(InvalidTransactionException.class, () -> 
                transactionService.createTransaction(testTransaction));
        }
    }

    @Nested
    @DisplayName("Transaction Status Update Tests")
    class TransactionStatusUpdateTests {
        @Test
        @DisplayName("Should update transaction status successfully")
        void shouldUpdateTransactionStatus() {
            Transaction created = transactionService.createTransaction(testTransaction);
            Transaction updated = transactionService.updateTransactionStatus(
                created.getTransactionId(), TransactionStatus.SUCCESS);

            assertEquals(TransactionStatus.SUCCESS, updated.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateTransactionStatus("non-existent", TransactionStatus.SUCCESS));
        }

        @Test
        @DisplayName("Should throw exception when updating non-running transaction")
        void shouldThrowExceptionWhenUpdatingNonRunningTransaction() {
            Transaction created = transactionService.createTransaction(testTransaction);
            transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.SUCCESS);
            
            assertThrows(InvalidTransactionStateException.class, () ->
                transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.FAILED));
        }

        @Test
        @DisplayName("Should throw exception when setting status to running")
        void shouldThrowExceptionWhenSettingStatusToRunning() {
            Transaction created = transactionService.createTransaction(testTransaction);
            
            assertThrows(InvalidTransactionStateException.class, () ->
                transactionService.updateTransactionStatus(created.getTransactionId(), TransactionStatus.RUNNING));
        }
    }

    @Nested
    @DisplayName("Transaction Query Tests")
    class TransactionQueryTests {
        @Test
        @DisplayName("Should get transactions by account")
        void shouldGetTransactionsByAccount() {
            Transaction created = transactionService.createTransaction(testTransaction);
            List<Transaction> transactions = transactionService.getTransactionsByAccount("123456");

            assertFalse(transactions.isEmpty());
            assertEquals(created.getTransactionId(), transactions.get(0).getTransactionId());
        }

        @Test
        @DisplayName("Should get transaction by ID")
        void shouldGetTransactionById() {
            Transaction created = transactionService.createTransaction(testTransaction);
            Transaction retrieved = transactionService.getTransaction(created.getTransactionId());

            assertEquals(created.getTransactionId(), retrieved.getTransactionId());
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent transaction")
        void shouldThrowExceptionWhenGettingNonExistentTransaction() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransaction("non-existent"));
        }

        @Test
        @DisplayName("Should query transactions with filters")
        void shouldQueryTransactionsWithFilters() {
            // Create multiple transactions
            Transaction created1 = transactionService.createTransaction(testTransaction);
            Transaction created2 = transactionService.createTransaction(
                Transaction.builder()
                    .accountNo("123456")
                    .amount(new BigDecimal("200.00"))
                    .direction(TransactionDirection.IN)
                    .build()
            );

            // Create filter
            TransactionFilter filter = TransactionFilter.builder()
                .accountNo("123456")
                .direction(TransactionDirection.OUT)
                .build();

            // Query with filter
            PageResponse<Transaction> response = transactionService.queryTransactions(filter, 0, 10);

            assertEquals(1, response.getTotalElements());
            assertEquals(created1.getTransactionId(), response.getContent().get(0).getTransactionId());
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Create multiple transactions
            for (int i = 0; i < 15; i++) {
                transactionService.createTransaction(
                    Transaction.builder()
                        .accountNo("123456")
                        .amount(new BigDecimal("100.00"))
                        .direction(TransactionDirection.OUT)
                        .build()
                );
            }

            // Test first page
            PageResponse<Transaction> firstPage = transactionService.queryTransactions(null, 0, 10);
            assertEquals(10, firstPage.getContent().size());
            assertTrue(firstPage.isFirst());
            assertFalse(firstPage.isLast());

            // Test second page
            PageResponse<Transaction> secondPage = transactionService.queryTransactions(null, 1, 10);
            assertEquals(5, secondPage.getContent().size());
            assertFalse(secondPage.isFirst());
            assertTrue(secondPage.isLast());
        }

        @Test
        @DisplayName("Should handle empty result set")
        void shouldHandleEmptyResultSet() {
            TransactionFilter filter = TransactionFilter.builder()
                .accountNo("non-existent")
                .build();

            PageResponse<Transaction> response = transactionService.queryTransactions(filter, 0, 10);

            assertTrue(response.getContent().isEmpty());
            assertEquals(0, response.getTotalElements());
            assertEquals(0, response.getTotalPages());
        }
    }

    @Nested
    @DisplayName("Transaction Update and Delete Tests")
    class TransactionUpdateAndDeleteTests {
        @Test
        @DisplayName("Should update transaction")
        void shouldUpdateTransaction() {
            Transaction created = transactionService.createTransaction(testTransaction);
            created.setAmount(new BigDecimal("200.00"));
            
            Transaction updated = transactionService.updateTransaction(
                created.getTransactionId(), created);

            assertEquals(new BigDecimal("200.00"), updated.getAmount());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent transaction")
        void shouldThrowExceptionWhenUpdatingNonExistentTransaction() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateTransaction("non-existent", testTransaction));
        }

        @Test
        @DisplayName("Should delete transaction")
        void shouldDeleteTransaction() {
            Transaction created = transactionService.createTransaction(testTransaction);
            
            transactionService.deleteTransaction(created.getTransactionId());
            
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransaction(created.getTransactionId()));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent transaction")
        void shouldThrowExceptionWhenDeletingNonExistentTransaction() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.deleteTransaction("non-existent"));
        }
    }
} 