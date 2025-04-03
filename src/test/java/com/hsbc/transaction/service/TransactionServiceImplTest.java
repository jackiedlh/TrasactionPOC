package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
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
                .accountNo("12345678901")
                .description("Test Transaction")
                .direction(TransactionDirection.OUT)
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("Should create a new transaction successfully")
    void createTransaction_ShouldCreateSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);

        assertNotNull(created.getId(), "Transaction ID should not be null");
        assertEquals(testTransaction.getAmount(), created.getAmount());
        assertEquals(testTransaction.getAccountNo(), created.getAccountNo());
        assertEquals(testTransaction.getDescription(), created.getDescription());
        assertEquals(testTransaction.getDirection(), created.getDirection());
        assertEquals(testTransaction.getStatus(), created.getStatus());
    }

    @Test
    @DisplayName("Should retrieve transaction by ID successfully")
    void getTransaction_ShouldRetrieveSuccessfully() {
        Transaction created = transactionService.createTransaction(testTransaction);
        Transaction retrieved = transactionService.getTransaction(created.getId());

        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getAmount(), retrieved.getAmount());
        assertEquals(created.getAccountNo(), retrieved.getAccountNo());
        assertEquals(created.getDescription(), retrieved.getDescription());
        assertEquals(created.getDirection(), retrieved.getDirection());
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
                .accountNo("98765432100")
                .description("Second Transaction")
                .direction(TransactionDirection.IN)
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
                .accountNo("11122233344")
                .description("Updated Transaction")
                .direction(TransactionDirection.IN)
                .status("COMPLETED")
                .build();

        Transaction updated = transactionService.updateTransaction(id, updateData);

        assertEquals(id, updated.getId(), "ID should remain the same");
        assertEquals(new BigDecimal("150.75"), updated.getAmount());
        assertEquals("11122233344", updated.getAccountNo());
        assertEquals("Updated Transaction", updated.getDescription());
        assertEquals(TransactionDirection.IN, updated.getDirection());
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
    @DisplayName("Should retrieve transactions by direction successfully")
    void getTransactionsByDirection_ShouldRetrieveSuccessfully() {
        // Create transactions with different directions
        transactionService.createTransaction(testTransaction); // OUT
        transactionService.createTransaction(Transaction.builder()
                .amount(new BigDecimal("200.00"))
                .accountNo("55566677788")
                .description("Deposit Transaction")
                .direction(TransactionDirection.IN)
                .status("COMPLETED")
                .build());
        transactionService.createTransaction(Transaction.builder()
                .amount(new BigDecimal("300.00"))
                .accountNo("99988877766")
                .description("Another Payment")
                .direction(TransactionDirection.OUT)
                .status("COMPLETED")
                .build());

        List<Transaction> outgoingTransactions = transactionService.getTransactionsByDirection(TransactionDirection.OUT.name());
        
        assertEquals(2, outgoingTransactions.size(), "Should return 2 OUT transactions");
        assertTrue(outgoingTransactions.stream()
                .allMatch(t -> TransactionDirection.OUT.equals(t.getDirection())),
                "All transactions should be of direction OUT");
    }

    @Test
    @DisplayName("Should return empty list for non-existent transaction direction")
    void getTransactionsByDirection_ShouldReturnEmptyList_WhenDirectionNotFound() {
        List<Transaction> transactions = transactionService.getTransactionsByDirection("INVALID_DIRECTION");
        assertTrue(transactions.isEmpty(), "Should return empty list for non-existent direction");
    }
} 