package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.exception.InvalidTransactionStateException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.BusinessService;
import com.hsbc.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class BusinessIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BusinessService businessService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Map<String, BigDecimal> accountStore;

    @BeforeEach
    void setUp() {
        // Initialize services
        accountStore = new ConcurrentHashMap<>();

        // Create test accounts
        accountStore.put("ACC001", new BigDecimal("1000.00"));
        accountStore.put("ACC002", new BigDecimal("500.00"));

        // Set account store in account service
        //accountService.setAccountBalances(accountStore);


    }

    @Test
    @DisplayName("Normal Path: Successful transfer between accounts")
    void normalPathSuccessfulTransfer() {
        org.springframework.transaction.TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        // Step 1: Create transaction for Account A (debit 200)
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();



        // Step 2: Create transaction for Account B (credit 200)
        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();

       businessService.combine(Arrays.asList(transactionA, transactionB));


        // Step 5: Verify account balances
        assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("700.00"), accountService.getBalance("ACC002"));
    }

    @Test
    @DisplayName("Abnormal Path: Insufficient funds in source account")
    void abnormalPathInsufficientFunds() {
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("1200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();
        Transaction createdTransactionA = transactionService.createTransaction(transactionA);

        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("1200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();
        Transaction createdTransactionB = transactionService.createTransaction(transactionB);

        assertThrows(InsufficientBalanceException.class, ()->
            businessService.combine(Arrays.asList(transactionA, transactionB)));


        // Verify account balances remain unchanged
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
    }

    @Test
    @DisplayName("Abnormal Path: Transaction B fails after Transaction A succeeds")
    @Transactional
    void abnormalPathTransactionBFails() {
        // Create transaction A (debit 200 from ACC001)
        Transaction transactionA = Transaction.builder()
                .accountNo("ACC001")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.DEBIT)
                .build();
        Transaction createdTransactionA = transactionService.createTransaction(transactionA);

        // Create transaction B (credit 200 to ACC002)
        Transaction transactionB = Transaction.builder()
                .accountNo("ACC002")
                .amount(new BigDecimal("200.00"))
                .direction(TransactionDirection.CREDIT)
                .build();
        Transaction createdTransactionB = transactionService.createTransaction(transactionB);
        transactionService.updateTransactionStatus(transactionB.getTransactionId(),TransactionStatus.FAILED);
//        createdTransactionB.setStatus(TransactionStatus.FAILED);

        assertThrows(InvalidTransactionStateException.class, ()->
            businessService.combine(Arrays.asList(transactionA, transactionB)));

        // Verify account balances remain unchanged
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
    }
} 