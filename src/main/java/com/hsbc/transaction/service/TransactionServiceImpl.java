package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.Transaction;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final Map<String, Transaction> transactionStore = new ConcurrentHashMap<>();

    @Override
    public Transaction createTransaction(Transaction transaction) {
        String id = UUID.randomUUID().toString();
        transaction.setId(id);
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
        if (!transactionStore.containsKey(id)) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }
        transaction.setId(id);
        transactionStore.put(id, transaction);
        return transaction;
    }

    @Override
    @CacheEvict(value = {"transactions", "allTransactions"}, allEntries = true)
    public void deleteTransaction(String id) {
        if (!transactionStore.containsKey(id)) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }
        transactionStore.remove(id);
    }

    @Override
    @Cacheable(value = "transactionsByType", key = "#type")
    public List<Transaction> getTransactionsByType(String type) {
        return transactionStore.values().stream()
                .filter(transaction -> type.equals(transaction.getType()))
                .collect(Collectors.toList());
    }
} 