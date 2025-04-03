package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.model.PageResponse;
import com.hsbc.transaction.model.TransactionFilter;

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

    /**
     * Get a transaction by ID
     * @param id The transaction ID
     * @return The transaction
     */
    Transaction getTransaction(String id);

    /**
     * Query transactions with optional filters and pagination
     * @param filter Optional filters for transactions
     * @param page The page number (0-based)
     * @param size The page size
     * @return PageResponse containing the filtered transactions and pagination information
     */
    PageResponse<Transaction> queryTransactions(TransactionFilter filter, int page, int size);

    /**
     * Update an existing transaction
     * @param id The transaction ID
     * @param transaction The updated transaction data
     * @return The updated transaction
     */
    Transaction updateTransaction(String id, Transaction transaction);

    /**
     * Delete a transaction
     * @param id The transaction ID
     */
    void deleteTransaction(String id);
} 