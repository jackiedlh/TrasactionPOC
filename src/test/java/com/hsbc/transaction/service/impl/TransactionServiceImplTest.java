package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.InvalidTransactionException;
import com.hsbc.transaction.exception.InvalidTransactionStateException;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceImplTest {
    private TransactionServiceImpl transactionService;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();

        testTransaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("123456")
                .amount(new BigDecimal("100.00"))
                .direction(TransactionDirection.DEBIT)
                .build();

    }

    @Nested
    @DisplayName("Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction successfully")
        void shouldCreateTransaction() {
            // Arrange
            Transaction transaction = Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build();

            // Act
            Transaction created = transactionService.createTransaction(transaction);

            // Assert
            assertNotNull(created.getTransactionId());
            assertEquals(TransactionStatus.RUNNING, created.getStatus());
            assertNotNull(created.getTimestamp());
            assertEquals("ACC001", created.getAccountNo());
            assertEquals(new BigDecimal("100.00"), created.getAmount());
            assertEquals(TransactionDirection.DEBIT, created.getDirection());
        }

        @Test
        @DisplayName("Should throw exception for null transaction")
        void shouldThrowExceptionForNullTransaction() {
            assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(null));
        }

        @Test
        @DisplayName("Should throw exception for invalid transaction")
        void shouldThrowExceptionForInvalidTransaction() {
            // Missing account number
            assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(Transaction.builder()
                    .amount(BigDecimal.ONE)
                    .direction(TransactionDirection.DEBIT)
                    .build()));

            // Missing amount
            assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(Transaction.builder()
                    .accountNo("ACC001")
                    .direction(TransactionDirection.DEBIT)
                    .build()));

            // Zero amount
            assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(Transaction.builder()
                    .accountNo("ACC001")
                    .amount(BigDecimal.ZERO)
                    .direction(TransactionDirection.DEBIT)
                    .build()));

            // Missing direction
            assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(Transaction.builder()
                    .accountNo("ACC001")
                    .amount(BigDecimal.ONE)
                    .build()));
        }
    }

    @Nested
    @DisplayName("Query Transactions Tests")
    class QueryTransactionTests {

        @BeforeEach
        void setUp() {
            // Create test transactions
            createTestTransaction("ACC001", "100.00", TransactionDirection.DEBIT);
            createTestTransaction("ACC001", "200.00", TransactionDirection.CREDIT);
            createTestTransaction("ACC002", "300.00", TransactionDirection.DEBIT);
        }

        private Transaction createTestTransaction(String accountNo, String amount, TransactionDirection direction) {
            Transaction transaction = Transaction.builder()
                    .accountNo(accountNo)
                    .amount(new BigDecimal(amount))
                    .direction(direction)
                    .build();
            return transactionService.createTransaction(transaction);
        }

        @Test
        @DisplayName("Should query transactions with filter")
        void shouldQueryTransactionsWithFilter() {
            // Arrange
            TransactionFilter filter = TransactionFilter.builder()
                    .accountNo("ACC001")
                    .build();

            // Act
            PageResponse<Transaction> response = transactionService.queryTransactions(filter, 0, 10);

            // Assert
            assertEquals(2, response.getContent().size());
            assertEquals(2, response.getTotalElements());
            assertEquals(1, response.getTotalPages());
            assertTrue(response.isFirst());
            assertTrue(response.isLast());
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Act
            PageResponse<Transaction> response = transactionService.queryTransactions(null, 0, 2);

            // Assert
            assertEquals(2, response.getContent().size());
            assertEquals(3, response.getTotalElements());
            assertEquals(2, response.getTotalPages());
            assertTrue(response.isFirst());
            assertFalse(response.isLast());
        }
    }

    @Nested
    @DisplayName("Update Transaction Status Tests")
    class UpdateTransactionStatusTests {

        @Test
        @DisplayName("Should update transaction status successfully")
        void shouldUpdateTransactionStatus() {
            Transaction created = transactionService.createTransaction(testTransaction);
            // Act
            Transaction updated = transactionService.updateTransactionStatus(
                testTransaction.getTransactionId(), 
                TransactionStatus.SUCCESS
            );

            // Assert
            assertEquals(TransactionStatus.SUCCESS, updated.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent transaction")
        void shouldThrowExceptionWhenUpdatingNonExistentTransaction() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateTransactionStatus("invalid-id", TransactionStatus.SUCCESS));
        }

        @Test
        @DisplayName("Should throw exception when updating non-RUNNING transaction")
        void shouldThrowExceptionWhenUpdatingNonRunningTransaction() {
            Transaction created = transactionService.createTransaction(testTransaction);
            // First update to SUCCESS
            transactionService.updateTransactionStatus(
                testTransaction.getTransactionId(), 
                TransactionStatus.SUCCESS
            );

            // Try to update again
            assertThrows(InvalidTransactionStateException.class, () ->
                transactionService.updateTransactionStatus(
                    testTransaction.getTransactionId(), 
                    TransactionStatus.FAILED
                ));
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete transaction successfully")
        void shouldDeleteTransaction() {
            // Arrange
            Transaction transaction = transactionService.createTransaction(Transaction.builder()
                    .accountNo("ACC001")
                    .amount(new BigDecimal("100.00"))
                    .direction(TransactionDirection.DEBIT)
                    .build());

            // Act
            transactionService.deleteTransaction(transaction.getTransactionId());

            // Assert
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransactionOrThrow(transaction.getTransactionId()));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent transaction")
        void shouldThrowExceptionWhenDeletingNonExistentTransaction() {
            assertThrows(TransactionNotFoundException.class, () ->
                transactionService.deleteTransaction("invalid-id"));
        }
    }
} 