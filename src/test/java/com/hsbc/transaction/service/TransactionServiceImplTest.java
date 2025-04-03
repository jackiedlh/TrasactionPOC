package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceImplTest {

    private TransactionService transactionService;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();
        testTransaction = Transaction.builder()
                .amount(new BigDecimal("100.50"))
                .description("Test Transaction")
                .type("PAYMENT")
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("Should create a new transaction successfully")
    void createTransaction_ShouldCreateSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);

        assertNotNull(created.getId(), "Transaction ID should not be null");
        assertEquals(testTransaction.getAmount(), created.getAmount());
        assertEquals(testTransaction.getDescription(), created.getDescription());
        assertEquals(testTransaction.getType(), created.getType());
        assertEquals(testTransaction.getStatus(), created.getStatus());
    }

    @Test
    @DisplayName("Should retrieve transaction by ID successfully")
    void getTransaction_ShouldRetrieveSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);
        Transaction retrieved = transactionService.getTransaction(created.getId());

        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getAmount(), retrieved.getAmount());
        assertEquals(created.getDescription(), retrieved.getDescription());
    }

    @Test
    @DisplayName("Should throw exception when retrieving non-existent transaction")
    void getTransaction_ShouldThrowException_WhenNotFound() {
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransaction("non-existent-id"),
                "Should throw TransactionNotFoundException for non-existent ID");
    }

    @Test
    @DisplayName("Should list all transactions successfully")
    void getAllTransactions_ShouldListAllSuccessfully() {
        // Create multiple transactions
        transactionService.createTransaction(testTransaction);
        transactionService.createTransaction(Transaction.builder()
                .amount(new BigDecimal("200.00"))
                .description("Second Transaction")
                .type("DEPOSIT")
                .status("COMPLETED")
                .build());

        List<Transaction> transactions = transactionService.getAllTransactions();
        
        assertEquals(2, transactions.size(), "Should return 2 transactions");
        assertTrue(transactions.stream()
                .anyMatch(t -> t.getDescription().equals("Test Transaction")),
                "Should contain first transaction");
        assertTrue(transactions.stream()
                .anyMatch(t -> t.getDescription().equals("Second Transaction")),
                "Should contain second transaction");
    }

    @Test
    @DisplayName("Should update transaction successfully")
    void updateTransaction_ShouldUpdateSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);
        String id = created.getId();

        Transaction updateData = Transaction.builder()
                .amount(new BigDecimal("150.75"))
                .description("Updated Transaction")
                .type("PAYMENT")
                .status("COMPLETED")
                .build();

        Transaction updated = transactionService.updateTransaction(id, updateData);

        assertEquals(id, updated.getId(), "ID should remain the same");
        assertEquals(new BigDecimal("150.75"), updated.getAmount());
        assertEquals("Updated Transaction", updated.getDescription());
        assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent transaction")
    void updateTransaction_ShouldThrowException_WhenNotFound() {
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.updateTransaction("non-existent-id", testTransaction),
                "Should throw TransactionNotFoundException for non-existent ID");
    }

    @Test
    @DisplayName("Should delete transaction successfully")
    void deleteTransaction_ShouldDeleteSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);
        String id = created.getId();

        assertDoesNotThrow(() -> transactionService.deleteTransaction(id));
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransaction(id),
                "Should throw TransactionNotFoundException after deletion");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent transaction")
    void deleteTransaction_ShouldThrowException_WhenNotFound() {
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction("non-existent-id"),
                "Should throw TransactionNotFoundException for non-existent ID");
    }

    @Test
    @DisplayName("Should retrieve transactions by type successfully")
    void getTransactionsByType_ShouldRetrieveSuccessfully() {
        // Create transactions with different types
        transactionService.createTransaction(testTransaction); // PAYMENT
        transactionService.createTransaction(Transaction.builder()
                .amount(new BigDecimal("200.00"))
                .description("Deposit Transaction")
                .type("DEPOSIT")
                .status("COMPLETED")
                .build());
        transactionService.createTransaction(Transaction.builder()
                .amount(new BigDecimal("300.00"))
                .description("Another Payment")
                .type("PAYMENT")
                .status("COMPLETED")
                .build());

        List<Transaction> paymentTransactions = transactionService.getTransactionsByType("PAYMENT");
        
        assertEquals(2, paymentTransactions.size(), "Should return 2 PAYMENT transactions");
        assertTrue(paymentTransactions.stream()
                .allMatch(t -> "PAYMENT".equals(t.getType())),
                "All transactions should be of type PAYMENT");
    }

    @Test
    @DisplayName("Should return empty list for non-existent transaction type")
    void getTransactionsByType_ShouldReturnEmptyList_WhenTypeNotFound() {
        List<Transaction> transactions = transactionService.getTransactionsByType("NON_EXISTENT_TYPE");
        assertTrue(transactions.isEmpty(), "Should return empty list for non-existent type");
    }
} 