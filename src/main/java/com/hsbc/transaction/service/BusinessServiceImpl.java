package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BusinessServiceImpl implements BusinessService {
    private final AccountService accountService;
    private final TransactionService transactionService;

    public BusinessServiceImpl(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional
    public synchronized void transfer(String fromAccount, String toAccount, BigDecimal amount, String description) {
        try {

            // Create debit transaction record
            Transaction debitTransaction = Transaction.builder()
                    .accountNo(fromAccount)
                    .amount(amount)
                    .description(description + " - Transfer to " + toAccount)
                    .direction(TransactionDirection.OUT)
                    .status(TransactionStatus.RUNNING)
                    .build();

            // Create credit transaction record
            Transaction creditTransaction = Transaction.builder()
                    .accountNo(toAccount)
                    .amount(amount)
                    .description(description + " - Transfer from " + fromAccount)
                    .direction(TransactionDirection.IN)
                    .status(TransactionStatus.RUNNING)
                    .build();

            // Record both transactions
            transactionService.createTransaction(debitTransaction);
            transactionService.createTransaction(creditTransaction);

            transactionService.updateTransactionStatus(debitTransaction.getTransactionId(),TransactionStatus.SUCCESS);
            transactionService.updateTransactionStatus(creditTransaction.getTransactionId(),TransactionStatus.SUCCESS);


        } catch (Exception e) {
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }
} 