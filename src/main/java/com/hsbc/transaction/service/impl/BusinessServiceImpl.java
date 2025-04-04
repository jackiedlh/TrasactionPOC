package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.BusinessService;
import com.hsbc.transaction.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        processCombineTransactions(transactions);
    }

    private void processCombineTransactions(List<Transaction> transactions) {
        List<Transaction> revertTxs = new ArrayList<>();
        try {
            for (Transaction transaction : transactions) {
                transactionService.createTransaction(transaction);
                accountService.updateAccountBalance(transaction);
                transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
                revertTxs.add(Transaction.revertTransaction(transaction));
            }
        }catch (Exception e) {//simulate rollback
            for (Transaction transaction : revertTxs) {
                transactionService.createTransaction(transaction);
                accountService.updateAccountBalance(transaction);
                transactionService.updateTransactionStatus(transaction.getTransactionId(), TransactionStatus.SUCCESS);
            }
            throw e;
        }
    }




}