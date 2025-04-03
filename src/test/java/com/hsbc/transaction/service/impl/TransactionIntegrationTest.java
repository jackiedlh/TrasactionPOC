package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.exception.TransactionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransactionService;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionIntegrationTest {

    private TransactionService transactionService;
    private AccountService accountService;
    private Map<String, BigDecimal> accountStore;

    @BeforeEach
    void setUp() {
        // Initialize services
        transactionService = new TransactionServiceImpl();
        accountService = new AccountServiceImpl();
        accountStore = new ConcurrentHashMap<>();

        // Create test accounts
        accountStore.put("ACC001", new BigDecimal("1000.00"));
        accountStore.put("ACC002", new BigDecimal("500.00"));

        // Set account store in account service
        ((AccountServiceImpl) accountService).setAccountBalances(accountStore);
        ((TransactionServiceImpl)transactionService).setAccountService(accountService);
    }

    @Test
    @DisplayName("Normal Path: Successful transfer between accounts")
    void normalPathSuccessfulTransfer() {
        // Step 1: Create transaction for Account A (debit 200)
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();

        Transaction createdTransactionA = transactionService.createTransaction(transactionA);

        // Step 2: Create transaction for Account B (credit 200)
        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();

        Transaction createdTransactionB = transactionService.createTransaction(transactionB);

        // Step 3: Update transaction A to SUCCESS
        transactionService.updateTransactionStatus(createdTransactionA.getTransactionId(), TransactionStatus.SUCCESS);

        // Step 4: Update transaction B to SUCCESS
        transactionService.updateTransactionStatus(createdTransactionB.getTransactionId(), TransactionStatus.SUCCESS);

        // Step 5: Verify account balances
        assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("700.00"), accountService.getBalance("ACC002"));
    }

    @Test
    @DisplayName("Abnormal Path: Insufficient funds in source account")
    void abnormalPathInsufficientFunds() {
        // Step 1: Create transaction for Account A (debit 1200 - more than balance)
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("1200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();

        Transaction createdTransactionA = transactionService.createTransaction(transactionA);

        // Step 2: Create transaction for Account B (credit 1200)
        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("1200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();

        Transaction createdTransactionB = transactionService.createTransaction(transactionB);

        // Step 3: Update transaction A to SUCCESS (should fail due to insufficient funds)
        assertThrows(InsufficientBalanceException.class, () ->
            transactionService.updateTransactionStatus(createdTransactionA.getTransactionId(), TransactionStatus.SUCCESS));

        // Step 4: Update transaction B to FAILED (since transaction A failed)
        transactionService.updateTransactionStatus(createdTransactionB.getTransactionId(), TransactionStatus.SUCCESS);

        // Step 5: Verify account balances remain unchanged
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
    }

    @Test
    @DisplayName("Abnormal Path: Transaction B fails after Transaction A succeeds")
    void abnormalPathTransactionBFails() {
        // Step 1: Create transaction for Account A (debit 200)
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();

        Transaction createdTransactionA = transactionService.createTransaction(transactionA);

        // Step 2: Create transaction for Account B (credit 200)
        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();

        Transaction createdTransactionB = transactionService.createTransaction(transactionB);

        // Step 3: Update transaction A to SUCCESS
        transactionService.updateTransactionStatus(createdTransactionA.getTransactionId(), TransactionStatus.SUCCESS);

        // Step 4: Update transaction B to FAILED
        assertThrows(TransactionFailedException.class, () ->
             transactionService.updateTransactionStatus(createdTransactionB.getTransactionId(), TransactionStatus.FAILED));

        // Step 5: Verify account balances
        assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
    }
} 