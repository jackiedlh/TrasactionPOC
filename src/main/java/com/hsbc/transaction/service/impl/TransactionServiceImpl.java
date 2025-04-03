package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.TransactionFailedException;
import com.hsbc.transaction.model.*;
import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransactionService;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.exception.InvalidTransactionStateException;
import com.hsbc.transaction.exception.InvalidTransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final Map<String, Transaction> transactionStore = new ConcurrentHashMap<>();
    private final Map<String, Lock> transactionLocks = new ConcurrentHashMap<>();



    @Autowired
    private AccountService accountService;

    @Override
    public String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        validateTransaction(transaction);
        
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(generateTransactionId());
        }
        
        transaction.setStatus(TransactionStatus.RUNNING);
        transaction.setTimestamp(LocalDateTime.now());
        
        logger.info("Creating new transaction with ID: {}", transaction.getTransactionId());
        transactionStore.put(transaction.getTransactionId(), transaction);
        
        return transaction;
    }

    @Override
    @Transactional
    public Transaction updateTransactionStatus(String transactionId, TransactionStatus status) {
        Lock lock = getTransactionLock(transactionId);
        try {
            lock.lock();
            
            Transaction transaction = getTransactionOrThrow(transactionId);
            validateStatusTransition(transaction.getStatus(), status);
            
            logger.info("Updating transaction {} status from {} to {}", 
                transactionId, transaction.getStatus(), status);
            
            transaction.setStatus(status);
            if (status == TransactionStatus.SUCCESS) {
                updateAccountBalance(transaction);
            }
            transactionStore.put(transactionId, transaction);
            
            return transaction;
        } finally {
            lock.unlock();
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

    @Override
    public List<Transaction> getTransactionsByAccount(String accountNo) {
        logger.debug("Retrieving transactions for account: {}", accountNo);
        return transactionStore.values().stream()
                .filter(t -> t.getAccountNo().equals(accountNo))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Transaction getTransaction(String id) {
        Transaction transaction = transactionStore.get(id);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", id);
            throw new TransactionNotFoundException("Transaction not found: " + id);
        }
        return transaction;
    }

    @Override
    public PageResponse<Transaction> queryTransactions(TransactionFilter filter, int page, int size) {
        logger.debug("Querying transactions with filter: {}, page: {}, size: {}", filter, page, size);
        
        List<Transaction> filteredTransactions = transactionStore.values().stream()
                .filter(t -> matchesFilter(t, filter))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());

        int totalElements = filteredTransactions.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Ensure page is within valid range
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<Transaction> pageContent = filteredTransactions.subList(start, end);

        logger.debug("Found {} transactions matching filter", totalElements);
        
        return PageResponse.<Transaction>builder()
                .content(pageContent)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    private boolean matchesFilter(Transaction transaction, TransactionFilter filter) {
        if (filter == null) {
            return true;
        }

        return (filter.getAccountNo() == null || transaction.getAccountNo().equals(filter.getAccountNo())) &&
               (filter.getDirection() == null || transaction.getDirection() == filter.getDirection()) &&
               (filter.getStatus() == null || transaction.getStatus() == filter.getStatus()) &&
               (filter.getMinAmount() == null || transaction.getAmount().compareTo(filter.getMinAmount()) >= 0) &&
               (filter.getMaxAmount() == null || transaction.getAmount().compareTo(filter.getMaxAmount()) <= 0) &&
               (filter.getFromDate() == null || !transaction.getTimestamp().isBefore(filter.getFromDate())) &&
               (filter.getToDate() == null || !transaction.getTimestamp().isAfter(filter.getToDate()));
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new InvalidTransactionException("Transaction cannot be null");
        }
        if (transaction.getAccountNo() == null || transaction.getAccountNo().trim().isEmpty()) {
            throw new InvalidTransactionException("Account number is required");
        }
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }
        if (transaction.getDirection() == null) {
            throw new InvalidTransactionException("Transaction direction is required");
        }
    }

    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        if (currentStatus != TransactionStatus.RUNNING) {
            throw new InvalidTransactionStateException(
                String.format("Cannot update transaction status from %s to %s. Only RUNNING transactions can be updated.",
                    currentStatus, newStatus));
        }
        if (newStatus == TransactionStatus.RUNNING) {
            throw new InvalidTransactionStateException("Cannot set status to RUNNING");
        }else if(newStatus == TransactionStatus.FAILED){
            throw new TransactionFailedException("Transaction failed");
        }

    }

    private Transaction getTransactionOrThrow(String id) {
        Transaction transaction = transactionStore.get(id);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", id);
            throw new TransactionNotFoundException("Transaction not found: " + id);
        }
        return transaction;
    }

    private Lock getTransactionLock(String transactionId) {
        return transactionLocks.computeIfAbsent(transactionId, k -> new ReentrantLock());
    }

    @Override
    @Transactional
    public Transaction updateTransaction(String id, Transaction transaction) {
        Lock lock = getTransactionLock(id);
        try {
            lock.lock();
            
            Transaction existingTransaction = getTransactionOrThrow(id);
            validateTransaction(transaction);
            
            logger.info("Updating transaction: {}", id);
            transaction.setTransactionId(id);
            transactionStore.put(id, transaction);
            
            return transaction;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(String id) {
        Lock lock = getTransactionLock(id);
        try {
            lock.lock();
            
            Transaction transaction = getTransactionOrThrow(id);
            logger.info("Deleting transaction: {}", id);
            transactionStore.remove(id);
        } finally {
            lock.unlock();
        }
    }

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }
} 