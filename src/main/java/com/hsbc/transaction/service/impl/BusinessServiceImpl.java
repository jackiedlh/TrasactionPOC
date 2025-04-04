package com.hsbc.transaction.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.BusinessService;
import com.hsbc.transaction.service.TransactionService;

@Service
public class BusinessServiceImpl implements BusinessService {
    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceImpl.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Override
    @Transactional
    public void combine(List<Transaction> transactions) {
        if (transactions == null) {
            throw new IllegalArgumentException("Transaction list cannot be null");
        }
        if (transactions.isEmpty()) {
            return;
        }
        processCombineTransactions(transactions);
    }

    private void processCombineTransactions(List<Transaction> transactions) {
        List<Transaction> refundTnx = new ArrayList<>();
        try {
            for (Transaction transaction : transactions) {
                transactionService.createTransaction(transaction);
                accountService.updateAccountBalance(transaction);
                transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
                refundTnx.add(transaction);
            }
        }catch (Exception e) {//simulate rollback
            for (Transaction transaction : refundTnx) {
                Transaction refundTransaction = Transaction.revertTransaction(transaction);
                transactionService.createTransaction(refundTransaction);
                accountService.updateAccountBalance(refundTransaction);
                transactionService.updateTransactionStatus(refundTransaction.getTransactionId(), TransactionStatus.SUCCESS);
                transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.REFUNDED);
            }
            throw e;
        }
    }




}