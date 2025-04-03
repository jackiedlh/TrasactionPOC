package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    private BusinessServiceImpl businessService;

    @BeforeEach
    void setUp() {
        businessService = new BusinessServiceImpl(accountService, transactionService);
    }

    @Test
    void transfer_Success() {
        // Arrange
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test Transfer";

        // Act
        businessService.transfer(fromAccount, toAccount, amount, description);

        // Assert
        verify(accountService).debit(fromAccount, amount);
        verify(accountService).credit(toAccount, amount);
        verify(transactionService, times(2)).createTransaction(any(Transaction.class));
    }

    @Test
    void transfer_InsufficientBalance_ShouldRollback() {
        // Arrange
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test Transfer";

        doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(accountService).debit(fromAccount, amount);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            businessService.transfer(fromAccount, toAccount, amount, description)
        );

        verify(accountService).debit(fromAccount, amount);
        verify(accountService, never()).credit(toAccount, amount);
        verify(transactionService, never()).createTransaction(any(Transaction.class));
    }

    @Test
    void transfer_CreditFailure_ShouldRollback() {
        // Arrange
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test Transfer";

        doThrow(new RuntimeException("Credit failed"))
                .when(accountService).credit(toAccount, amount);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            businessService.transfer(fromAccount, toAccount, amount, description)
        );

        verify(accountService).debit(fromAccount, amount);
        verify(accountService).credit(toAccount, amount);
        verify(accountService).credit(fromAccount, amount); // Rollback
        verify(transactionService, never()).createTransaction(any(Transaction.class));
    }

    @Test
    void transfer_TransactionRecordFailure_ShouldRollback() {
        // Arrange
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test Transfer";

        doThrow(new RuntimeException("Transaction record failed"))
                .when(transactionService).createTransaction(any(Transaction.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            businessService.transfer(fromAccount, toAccount, amount, description)
        );

        verify(accountService).debit(fromAccount, amount);
        verify(accountService).credit(toAccount, amount);
        verify(accountService).credit(fromAccount, amount); // Rollback
        verify(transactionService).createTransaction(any(Transaction.class));
    }

    @Test
    void transfer_ConcurrentTransfers_ShouldBeThreadSafe() throws InterruptedException {
        // Arrange
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test Transfer";

        // Act
        Thread thread1 = new Thread(() -> businessService.transfer(fromAccount, toAccount, amount, description));
        Thread thread2 = new Thread(() -> businessService.transfer(fromAccount, toAccount, amount, description));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Assert
        verify(accountService, times(2)).debit(fromAccount, amount);
        verify(accountService, times(2)).credit(toAccount, amount);
        verify(transactionService, times(4)).createTransaction(any(Transaction.class));
    }
} 