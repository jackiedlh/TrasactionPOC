package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private AccountServiceImpl accountService;
    private ConcurrentHashMap<String, BigDecimal> balances;

    @BeforeEach
    void setUp() {
        balances = new ConcurrentHashMap<>();
        accountService = new AccountServiceImpl(balances);
    }

    @Test
    void getBalance_AccountExists_ShouldReturnBalance() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        balances.put(accountNo, initialBalance);

        // Act
        BigDecimal balance = accountService.getBalance(accountNo);

        // Assert
        assertEquals(initialBalance, balance);
    }

    @Test
    void getBalance_AccountDoesNotExist_ShouldReturnZero() {
        // Arrange
        String accountNo = "ACC001";

        // Act
        BigDecimal balance = accountService.getBalance(accountNo);

        // Assert
        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void credit_AccountExists_ShouldUpdateBalance() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal creditAmount = new BigDecimal("500.00");
        balances.put(accountNo, initialBalance);

        // Act
        accountService.credit(accountNo, creditAmount);

        // Assert
        assertEquals(initialBalance.add(creditAmount), balances.get(accountNo));
    }

    @Test
    void credit_AccountDoesNotExist_ShouldCreateAccount() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal creditAmount = new BigDecimal("500.00");

        // Act
        accountService.credit(accountNo, creditAmount);

        // Assert
        assertEquals(creditAmount, balances.get(accountNo));
    }

    @Test
    void debit_SufficientBalance_ShouldUpdateBalance() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("500.00");
        balances.put(accountNo, initialBalance);

        // Act
        accountService.debit(accountNo, debitAmount);

        // Assert
        assertEquals(initialBalance.subtract(debitAmount), balances.get(accountNo));
    }

    @Test
    void debit_InsufficientBalance_ShouldThrowException() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("1500.00");
        balances.put(accountNo, initialBalance);

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> 
            accountService.debit(accountNo, debitAmount)
        );
        assertEquals(initialBalance, balances.get(accountNo)); // Balance should remain unchanged
    }

    @Test
    void debit_AccountDoesNotExist_ShouldThrowException() {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal debitAmount = new BigDecimal("500.00");

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> 
            accountService.debit(accountNo, debitAmount)
        );
        assertFalse(balances.containsKey(accountNo));
    }

    @Test
    void concurrentOperations_ShouldBeThreadSafe() throws InterruptedException {
        // Arrange
        String accountNo = "ACC001";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        balances.put(accountNo, initialBalance);

        // Act
        Thread creditThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                accountService.credit(accountNo, BigDecimal.ONE);
            }
        });

        Thread debitThread = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                try {
                    accountService.debit(accountNo, BigDecimal.ONE);
                } catch (InsufficientBalanceException e) {
                    // Expected in some cases
                }
            }
        });

        creditThread.start();
        debitThread.start();

        creditThread.join();
        debitThread.join();

        // Assert
        BigDecimal expectedBalance = initialBalance.add(new BigDecimal("50")); // 100 credits - 50 debits
        assertEquals(expectedBalance, balances.get(accountNo));
    }
} 