//package com.hsbc.transaction.service.impl;
//
//import java.math.BigDecimal;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import com.hsbc.transaction.exception.InsufficientBalanceException;
//import com.hsbc.transaction.service.BusinessService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.TransactionDefinition;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.support.DefaultTransactionDefinition;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import com.hsbc.transaction.model.Transaction;
//import com.hsbc.transaction.model.TransactionDirection;
//import com.hsbc.transaction.model.TransactionStatus;
//import com.hsbc.transaction.service.AccountService;
//import com.hsbc.transaction.service.TransactionService;
//
//
//@SpringBootTest
//class TransactionIntegrationTest {
//
//    @Autowired
//    private TransactionService transactionService;
//
//    @Autowired
//    private AccountService accountService;
//
//    @Autowired
//    private BusinessService businessService;
//    @Autowired
//    private PlatformTransactionManager transactionManager;
//
//    private Map<String, BigDecimal> accountStore;
//    private TransactionTemplate transactionTemplate;
//
//    @BeforeEach
//    void setUp() {
//        // Initialize services
//        accountStore = new ConcurrentHashMap<>();
//
//        // Create test accounts
//        accountStore.put("ACC001", new BigDecimal("1000.00"));
//        accountStore.put("ACC002", new BigDecimal("500.00"));
//
//    }
//
//    @Test
//    @DisplayName("Normal Path: Successful transfer between accounts")
//    void normalPathSuccessfulTransfer() {
//        org.springframework.transaction.TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//
//        // Step 1: Create transaction for Account A (debit 200)
//        Transaction transactionA = Transaction.builder()
//                .accountNo("ACC001")
//                .amount(new BigDecimal("200.00"))
//                .direction(TransactionDirection.DEBIT)
//                .build();
//
//        Transaction createdTransactionA = transactionService.createTransaction(transactionA);
//
//        // Step 2: Create transaction for Account B (credit 200)
//        Transaction transactionB = Transaction.builder()
//                .accountNo("ACC002")
//                .amount(new BigDecimal("200.00"))
//                .direction(TransactionDirection.CREDIT)
//                .build();
//
//        Transaction createdTransactionB = transactionService.createTransaction(transactionB);
//
//        // Step 3: Update transaction A to SUCCESS
//        transactionService.updateTransactionStatus(createdTransactionA.getTransactionId(), TransactionStatus.SUCCESS);
//
//        // Step 4: Update transaction B to SUCCESS
//        transactionService.updateTransactionStatus(createdTransactionB.getTransactionId(), TransactionStatus.SUCCESS);
//
//        // Step 5: Verify account balances
//        assertEquals(new BigDecimal("800.00"), accountService.getBalance("ACC001"));
//        assertEquals(new BigDecimal("700.00"), accountService.getBalance("ACC002"));
//    }
//
//    @Test
//    @DisplayName("Abnormal Path: Insufficient funds in source account")
//    void abnormalPathInsufficientFunds() {
//        Transaction transactionA = Transaction.builder()
//                .accountNo("ACC001")
//                .amount(new BigDecimal("1200.00"))
//                .direction(TransactionDirection.DEBIT)
//                .build();
//        Transaction createdTransactionA = transactionService.createTransaction(transactionA);
//
//        Transaction transactionB = Transaction.builder()
//                .accountNo("ACC002")
//                .amount(new BigDecimal("1200.00"))
//                .direction(TransactionDirection.CREDIT)
//                .build();
//        Transaction createdTransactionB = transactionService.createTransaction(transactionB);
//
//        try {
//            transactionTemplate.execute(status -> {
//                // This will fail due to insufficient funds and trigger rollback
//                transactionService.updateTransactionStatus(
//                    createdTransactionA.getTransactionId(),
//                    TransactionStatus.SUCCESS
//                );
//
//                transactionService.updateTransactionStatus(
//                    createdTransactionB.getTransactionId(),
//                    TransactionStatus.SUCCESS
//                );
//
//                return null;
//            });
//        } catch (Exception e) {
//            // Transaction was automatically rolled back
//        }
//
//        // Verify account balances remain unchanged
//        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
//        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
//    }
//
//    @Test
//    @DisplayName("Abnormal Path: Transaction B fails after Transaction A succeeds")
//    @Transactional
//    void abnormalPathTransactionBFails() {
//        // Create transaction A (debit 200 from ACC001)
//        Transaction transactionA = Transaction.builder()
//                .accountNo("ACC001")
//                .amount(new BigDecimal("2000.00"))
//                .direction(TransactionDirection.DEBIT)
//                .build();
//        Transaction createdTransactionA = transactionService.createTransaction(transactionA);
//
//        // Create transaction B (credit 200 to ACC002)
//        Transaction transactionB = Transaction.builder()
//                .accountNo("ACC002")
//                .amount(new BigDecimal("200.00"))
//                .direction(TransactionDirection.CREDIT)
//                .build();
//        Transaction createdTransactionB = transactionService.createTransaction(transactionB);
//
//
////                // First update succeeds
////                transactionService.updateTransactionStatus(createdTransactionA.getTransactionId(), TransactionStatus.SUCCESS);
////
////                // Second update fails
////                transactionService.updateTransactionStatus(createdTransactionB.getTransactionId(), TransactionStatus.FAILED);
//        assertThrows(InsufficientBalanceException.class, ()->
//        transactionService.processTransfer(createdTransactionA.getTransactionId(), createdTransactionB.getTransactionId()));
//
//
//        // Verify both transactions are marked as FAILED
////        assertEquals(TransactionStatus.FAILED,
////            transactionService.getTransaction(createdTransactionA.getTransactionId()).getStatus());
////        assertEquals(TransactionStatus.FAILED,
////            transactionService.getTransaction(createdTransactionB.getTransactionId()).getStatus());
//
//        // Verify account balances remain unchanged
//        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
//        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
//    }
//}