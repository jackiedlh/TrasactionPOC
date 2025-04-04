package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.BusinessService;
import com.hsbc.transaction.service.TransactionService;

@Service
public class BusinessServiceImpl implements BusinessService {
    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceImpl.class);

    @Autowired
    private final TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    public BusinessServiceImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    @Transactional
    public void combine(List<Transaction> transactions) {
        processCombineTransactions(transactions);
    }

    private void processCombineTransactions(List<Transaction> transactions) {
        List<Transaction> originalTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            transactionService.createTransaction(transaction);
            transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
            accountService.updateAccountBalance(transaction);


        }
    }

    private void updateAccountBalance(Transaction transaction) {
        String accountNo = transaction.getAccountNo();
        BigDecimal amount = transaction.getAmount();

        if (transaction.getDirection() == TransactionDirection.DEBIT) {
            accountService.debit(accountNo, amount);
            logger.info("Debited {} from account {}", amount, accountNo);
        } else if (transaction.getDirection() == TransactionDirection.CREDIT) {
            accountService.credit(accountNo, amount);
            logger.info("Credited {} to account {}", amount, accountNo);
        }
    }
}