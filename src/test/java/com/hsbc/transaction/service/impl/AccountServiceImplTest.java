package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hsbc.transaction.exception.AccountAlwaysExistException;
import com.hsbc.transaction.exception.AccountNotFoundException;
import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;

class AccountServiceImplTest {

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl();
        // Create test accounts with initial balances
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));
        accountService.createAccount("ACC002", new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Create Account Tests")
    class CreateAccountTests {
        
        @Test
        @DisplayName("Should create account with initial balance successfully")
        void shouldCreateAccountWithInitialBalance() {
            // Arrange
            String accountNo = "ACC003";
            BigDecimal initBalance = new BigDecimal("750.00");
            
            // Act
            accountService.createAccount(accountNo, initBalance);
            
            // Assert
            assertEquals(initBalance, accountService.getBalance(accountNo));
        }

        @Test
        @DisplayName("Should create account with zero initial balance")
        void shouldCreateAccountWithZeroBalance() {
            // Arrange
            String accountNo = "ACC004";
            
            // Act
            accountService.createAccount(accountNo, BigDecimal.ZERO);
            
            // Assert
            assertEquals(BigDecimal.ZERO, accountService.getBalance(accountNo));
        }

        @Test
        @DisplayName("Should throw exception when creating account with null initial balance")
        void shouldThrowExceptionWhenCreatingAccountWithNullBalance() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccount("ACC005", null));
        }

        @Test
        @DisplayName("Should throw exception when creating account with negative initial balance")
        void shouldThrowExceptionWhenCreatingAccountWithNegativeBalance() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccount("ACC005", new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("Should throw exception when creating existing account")
        void shouldThrowExceptionWhenCreatingExistingAccount() {
            // Act & Assert
            assertThrows(AccountAlwaysExistException.class, () ->
                accountService.createAccount("ACC001", BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("Get Balance Tests")
    class GetBalanceTests {
        
        @Test
        @DisplayName("Should get balance successfully")
        void shouldGetBalance() {
            // Act
            BigDecimal balance = accountService.getBalance("ACC001");
            
            // Assert
            assertEquals(new BigDecimal("1000.00"), balance);
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            // Act & Assert
            assertThrows(AccountNotFoundException.class, () ->
                accountService.getBalance("INVALID_ACC"));
        }
    }

    @Nested
    @DisplayName("Credit Tests")
    class CreditTests {
        
        @Test
        @DisplayName("Should credit account successfully")
        void shouldCreditAccount() {


            // Arrange
            BigDecimal creditAmount = new BigDecimal("500.00");
            
            // Act
            accountService.credit("ACC001", creditAmount);
            
            // Assert
            assertEquals(new BigDecimal("1500.00"), accountService.getBalance("ACC001"));
        }

        @Test
        @DisplayName("Should throw exception when crediting non-existent account")
        void shouldThrowExceptionWhenCreditingNonExistentAccount() {
            // Act & Assert
            assertThrows(AccountNotFoundException.class, () ->
                accountService.credit("INVALID_ACC", BigDecimal.ONE));
        }
    }

    @Nested
    @DisplayName("Debit Tests")
    class DebitTests {
        
        @Test
        @DisplayName("Should debit account successfully")
        void shouldDebitAccount() {

            // Arrange
            BigDecimal debitAmount = new BigDecimal("400.00");
            
            // Act
            accountService.debit("ACC001", debitAmount);
            
            // Assert
            assertEquals(new BigDecimal("600.00"), accountService.getBalance("ACC001"));
        }

        @Test
        @DisplayName("Should throw exception when insufficient balance")
        void shouldThrowExceptionWhenInsufficientBalance() {


            // Arrange
            BigDecimal debitAmount = new BigDecimal("1500.00");
            
            // Act & Assert
            assertThrows(InsufficientBalanceException.class, () ->
                accountService.debit("ACC001", debitAmount));
            
            // Verify balance unchanged
            assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
        }

        @Test
        @DisplayName("Should throw exception when debiting non-existent account")
        void shouldThrowExceptionWhenDebitingNonExistentAccount() {
            // Act & Assert
            assertThrows(AccountNotFoundException.class, () ->
                accountService.debit("INVALID_ACC", BigDecimal.ONE));
        }
    }

    @Nested
    @DisplayName("Update Account Balance Tests")
    class UpdateAccountBalanceTests {
        
        @Test
        @DisplayName("Should update balance for debit transaction")
        void shouldUpdateBalanceForDebitTransaction() {


            // Arrange
            Transaction transaction = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("500.00"))
                .direction(TransactionDirection.DEBIT)
                .build();
            
            // Act
            accountService.updateAccountBalance(transaction);
            
            // Assert
            assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC001"));
        }

        @Test
        @DisplayName("Should update balance for credit transaction")
        void shouldUpdateBalanceForCreditTransaction() {

            // Arrange
            Transaction transaction = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("500.00"))
                .direction(TransactionDirection.CREDIT)
                .build();
            
            // Act
            accountService.updateAccountBalance(transaction);
            
            // Assert
            assertEquals(new BigDecimal("1500.00"), accountService.getBalance("ACC001"));
        }
    }

    @Nested
    @DisplayName("Initial Balance Tests")
    class InitialBalanceTests {
        
        @Test
        @DisplayName("Should maintain initial balance after creation")
        void shouldMaintainInitialBalance() {
            // Arrange
            String accountNo = "NEW_ACC";
            BigDecimal initBalance = new BigDecimal("1000.00");
            
            // Act
            accountService.createAccount(accountNo, initBalance);
            
            // Assert
            assertEquals(initBalance, accountService.getBalance(accountNo));
        }

        @Test
        @DisplayName("Should handle operations on account with initial balance")
        void shouldHandleOperationsWithInitialBalance() {
            // Arrange
            String accountNo = "TEST_ACC";
            BigDecimal initBalance = new BigDecimal("1000.00");
            accountService.createAccount(accountNo, initBalance);
            
            // Act
            accountService.credit(accountNo, new BigDecimal("500.00"));
            accountService.debit(accountNo, new BigDecimal("200.00"));
            
            // Assert
            BigDecimal expectedBalance = new BigDecimal("1300.00"); // 1000 + 500 - 200
            assertEquals(expectedBalance, accountService.getBalance(accountNo));
        }
    }

    @Nested
    @DisplayName("Concurrent Creation Tests")
    class ConcurrentCreationTests {
        
        @Test
        @DisplayName("Should handle concurrent account creations")
        void shouldHandleConcurrentCreations() throws InterruptedException {
            // Arrange
            int numberOfThreads = 10;
            Thread[] threads = new Thread[numberOfThreads];
            String[] accountNumbers = new String[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                accountNumbers[i] = "CONC_ACC_" + i;
            }
            
            // Act
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        accountService.createAccount(
                            accountNumbers[index], 
                            new BigDecimal("100.00")
                        );
                    } catch (Exception ignored) {
                        // Ignore exceptions for duplicate creation attempts
                    }
                });
                threads[i].start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Assert
            for (String accountNo : accountNumbers) {
                BigDecimal balance = accountService.getBalance(accountNo);
                assertEquals(new BigDecimal("100.00"), balance);
            }
        }
    }
} 