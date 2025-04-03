package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final Map<String, Transaction> transactionStore = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();

    private void updateAccountBalance(String accountNo, BigDecimal amount, TransactionDirection direction) {
        // Initialize account if not exists
        accountBalances.putIfAbsent(accountNo, BigDecimal.ZERO);
        
        BigDecimal currentBalance = accountBalances.get(accountNo);
        BigDecimal newBalance;
        
        if (TransactionDirection.IN.equals(direction)) {
            newBalance = currentBalance.add(amount);
        } else {
            newBalance = currentBalance.subtract(amount);
        }
        
        accountBalances.put(accountNo, newBalance);
    }

    public BigDecimal getAccountBalance(String accountNo) {
        return accountBalances.getOrDefault(accountNo, BigDecimal.ZERO);
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        String id = UUID.randomUUID().toString();
        transaction.setId(id);
        
        // Update account balance based on transaction direction
        updateAccountBalance(
            transaction.getAccountNo(),
            transaction.getAmount(),
            transaction.getDirection()
        );
        
        transactionStore.put(id, transaction);
        return transaction;
    }

    @Override
    @Cacheable(value = "transactions", key = "#id")
    public Transaction getTransaction(String id) {
        Transaction transaction = transactionStore.get(id);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }
        return transaction;
    }

    @Override
    @Cacheable(value = "allTransactions")
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactionStore.values());
    }

    @Override
    @CacheEvict(value = {"transactions", "allTransactions"}, allEntries = true)
    public Transaction updateTransaction(String id, Transaction transaction) {
        Transaction existingTransaction = transactionStore.get(id);
        if (existingTransaction == null) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }

        // Reverse the effect of the old transaction
        updateAccountBalance(
            existingTransaction.getAccountNo(),
            existingTransaction.getAmount(),
            existingTransaction.getDirection().equals(TransactionDirection.IN) ? 
                TransactionDirection.OUT : TransactionDirection.IN
        );

        // Apply the new transaction
        transaction.setId(id);
        updateAccountBalance(
            transaction.getAccountNo(),
            transaction.getAmount(),
            transaction.getDirection()
        );

        transactionStore.put(id, transaction);
        return transaction;
    }

    @Override
    @CacheEvict(value = {"transactions", "allTransactions"}, allEntries = true)
    public void deleteTransaction(String id) {
        Transaction existingTransaction = transactionStore.get(id);
        if (existingTransaction == null) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }

        // Reverse the effect of the transaction being deleted
        updateAccountBalance(
            existingTransaction.getAccountNo(),
            existingTransaction.getAmount(),
            existingTransaction.getDirection().equals(TransactionDirection.IN) ? 
                TransactionDirection.OUT : TransactionDirection.IN
        );

        transactionStore.remove(id);
    }

    @Override
    @Cacheable(value = "transactionsByDirection", key = "#direction")
    public List<Transaction> getTransactionsByDirection(String direction) {
        try {
            TransactionDirection transactionDirection = TransactionDirection.valueOf(direction);
            return transactionStore.values().stream()
                    .filter(transaction -> transactionDirection.equals(transaction.getDirection()))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }
} 