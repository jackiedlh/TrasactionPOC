package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    @Autowired
    AccountService accountService;

    @Override
    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(generateTransactionId());
        }
        
        // Validate transaction ID format
        try {
            UUID.fromString(transaction.getTransactionId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction ID format");
        }

        // Set initial status and timestamp if not provided
        if (transaction.getStatus() == null) {
            transaction.setStatus(TransactionStatus.RUNNING);
        }
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }

        transactions.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    @Override
    public Transaction updateTransactionStatus(String transactionId, TransactionStatus newStatus) {
        Transaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + transactionId);
        }

        // Only allow status updates for RUNNING transactions
        if (transaction.getStatus() != TransactionStatus.RUNNING) {
            throw new IllegalStateException("Cannot update status of transaction in " + transaction.getStatus() + " state");
        }

        // Only allow updates to FAILED or SUCCESS
        if (newStatus != TransactionStatus.FAILED && newStatus != TransactionStatus.SUCCESS) {
            throw new IllegalArgumentException("Can only update status to FAILED or SUCCESS");
        }

        if (TransactionStatus.SUCCESS.equals(newStatus)) {
            switch(transaction.getDirection()) {
                case TransactionDirection.OUT ->
                        accountService.debit(transaction.getAccountNo(), transaction.getAmount());
                case TransactionDirection.IN ->
                        accountService.credit(transaction.getAccountNo(), transaction.getAmount());
                default -> throw new IllegalArgumentException("Invalid direction");
            }
        }

        transaction.setStatus(newStatus);
        return transaction;
    }

    @Override
    public List<Transaction> getTransactionsByAccount(String accountNo) {
        return transactions.values().stream()
                .filter(t -> t.getAccountNo().equals(accountNo))
                .toList();
    }

    @Override
    public Transaction getTransaction(String id) {
        Transaction transaction = transactions.get(id);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + id);
        }
        return transaction;
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions.values());
    }

    @Override
    public Transaction updateTransaction(String id, Transaction transaction) {
        Transaction existingTransaction = transactions.get(id);
        if (existingTransaction == null) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + id);
        }

        // Preserve the original transaction ID and status
        transaction.setTransactionId(id);
        transaction.setStatus(existingTransaction.getStatus());
        
        transactions.put(id, transaction);
        return transaction;
    }

    @Override
    public void deleteTransaction(String id) {
        if (!transactions.containsKey(id)) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + id);
        }
        transactions.remove(id);
    }
} 