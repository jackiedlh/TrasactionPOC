package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransactionService;
import com.hsbc.transaction.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferServiceImpl implements TransferService {
    private final AccountService accountService;
    private final TransactionService transactionService;

    public TransferServiceImpl(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional
    public synchronized void transfer(String fromAccount, String toAccount, BigDecimal amount, String description) {
        try {
            // First debit from source account (this will check for sufficient balance)
            accountService.debit(fromAccount, amount);
            
            // Then credit to destination account
            accountService.credit(toAccount, amount);

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
        } catch (Exception e) {
            // If anything fails, try to rollback the debit operation
            try {
                accountService.credit(fromAccount, amount);
            } catch (Exception rollbackException) {
                throw new RuntimeException("Transfer failed and rollback failed. Account states may be inconsistent", rollbackException);
            }
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }
} 