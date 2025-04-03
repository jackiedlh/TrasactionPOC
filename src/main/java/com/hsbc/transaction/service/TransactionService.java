package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;
import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    Transaction createTransaction(Transaction transaction);
    Transaction getTransaction(String id);
    List<Transaction> getAllTransactions();
    Transaction updateTransaction(String id, Transaction transaction);
    void deleteTransaction(String id);
    List<Transaction> getTransactionsByDirection(String direction);
    BigDecimal getAccountBalance(String accountNo);
} 