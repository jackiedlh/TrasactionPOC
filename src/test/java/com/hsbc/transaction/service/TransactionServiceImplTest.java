package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private AccountService accountService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();
        transactionService.accountService = accountService;
    }

    @Test
    void createTransaction_WithValidData_ShouldCreateTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .build();

        // Act
        Transaction created = transactionService.createTransaction(transaction);

        // Assert
        assertNotNull(created.getTransactionId());
        assertTrue(isValidUUID(created.getTransactionId()));
        assertEquals(TransactionStatus.RUNNING, created.getStatus());
        assertNotNull(created.getTimestamp());
        assertEquals(transaction.getAccountNo(), created.getAccountNo());
        assertEquals(transaction.getAmount(), created.getAmount());
        assertEquals(transaction.getDescription(), created.getDescription());
        assertEquals(transaction.getDirection(), created.getDirection());
    }

    @Test
    void createTransaction_WithExistingId_ShouldValidateUUID() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId("invalid-uuid")
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            transactionService.createTransaction(transaction)
        );
    }

    @Test
    void updateTransactionStatus_WithValidStatus_ShouldUpdateStatus() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.RUNNING)
                .build();
        transactionService.createTransaction(transaction);

        // Act
        Transaction updated = transactionService.updateTransactionStatus(
                transaction.getTransactionId(), 
                TransactionStatus.SUCCESS
        );

        // Assert
        assertEquals(TransactionStatus.SUCCESS, updated.getStatus());
        verify(accountService).credit(transaction.getAccountNo(), transaction.getAmount());
    }

    @Test
    void updateTransactionStatus_WithInvalidStatus_ShouldThrowException() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.RUNNING)
                .build();
        transactionService.createTransaction(transaction);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            transactionService.updateTransactionStatus(
                    transaction.getTransactionId(), 
                    TransactionStatus.RUNNING
            )
        );
        verify(accountService, never()).credit(any(), any());
    }

    @Test
    void updateTransactionStatus_WithNonRunningStatus_ShouldThrowException() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.SUCCESS)
                .build();
        transactionService.createTransaction(transaction);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            transactionService.updateTransactionStatus(
                    transaction.getTransactionId(), 
                    TransactionStatus.FAILED
            )
        );
        verify(accountService, never()).credit(any(), any());
    }

    @Test
    void updateTransactionStatus_WithOutDirection_ShouldDebitAccount() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.OUT)
                .status(TransactionStatus.RUNNING)
                .build();
        transactionService.createTransaction(transaction);

        // Act
        transactionService.updateTransactionStatus(
                transaction.getTransactionId(), 
                TransactionStatus.SUCCESS
        );

        // Assert
        verify(accountService).debit(transaction.getAccountNo(), transaction.getAmount());
    }

    @Test
    void getTransaction_WithValidId_ShouldReturnTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction")
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.RUNNING)
                .build();
        transactionService.createTransaction(transaction);

        // Act
        Transaction found = transactionService.getTransaction(transaction.getTransactionId());

        // Assert
        assertEquals(transaction, found);
    }

    @Test
    void getTransaction_WithInvalidId_ShouldThrowException() {
        // Act & Assert
        assertThrows(TransactionNotFoundException.class, () -> 
            transactionService.getTransaction("non-existent-id")
        );
    }

    @Test
    void getTransactionsByAccount_ShouldReturnMatchingTransactions() {
        // Arrange
        Transaction transaction1 = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC001")
                .amount(new BigDecimal("100.00"))
                .description("Test Transaction 1")
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.RUNNING)
                .build();
        Transaction transaction2 = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo("ACC002")
                .amount(new BigDecimal("200.00"))
                .description("Test Transaction 2")
                .direction(TransactionDirection.OUT)
                .status(TransactionStatus.RUNNING)
                .build();
        transactionService.createTransaction(transaction1);
        transactionService.createTransaction(transaction2);

        // Act
        List<Transaction> transactions = transactionService.getTransactionsByAccount("ACC001");

        // Assert
        assertEquals(1, transactions.size());
        assertEquals(transaction1, transactions.get(0));
    }

    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 