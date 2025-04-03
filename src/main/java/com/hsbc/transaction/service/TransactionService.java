package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    /**
     * Generate a new transaction ID
     * @return A new UUID string
     */
    default String generateTransactionId(){
        return UUID.randomUUID().toString();
    }

    /**
     * Create a new transaction
     * @param transaction The transaction to create. If transactionId is not provided, a new one will be generated
     * @return The created transaction
     */
    Transaction createTransaction(Transaction transaction);

    /**
     * Update an existing transaction's status
     * @param transactionId The ID of the transaction to update
     * @param status The new status. Only RUNNING transactions can be updated to FAILED or SUCCESS
     * @return The updated transaction
     */
    Transaction updateTransactionStatus(String transactionId, TransactionStatus status);

    /**
     * Get all transactions for an account
     * @param accountNo The account number
     * @return List of transactions for the account
     */
    List<Transaction> getTransactionsByAccount(String accountNo);

    Transaction getTransaction(String id);
    List<Transaction> getAllTransactions();
    Transaction updateTransaction(String id, Transaction transaction);
    void deleteTransaction(String id);

} 