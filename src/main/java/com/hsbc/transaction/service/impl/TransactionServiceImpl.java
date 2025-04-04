package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.exception.InvalidTransactionException;
import com.hsbc.transaction.exception.InvalidTransactionStateException;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.model.PageResponse;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionFilter;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.service.TransactionService;

@Service
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final ConcurrentHashMap<String, Transaction> transactionStore = new ConcurrentHashMap<>();



    @Override
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        validateTransaction(transaction);

        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(generateTransactionId());
        }

        if (transaction.getStatus() == null) {
            transaction.setStatus(TransactionStatus.RUNNING);
        }

        transaction.setTimestamp(LocalDateTime.now());

        logger.info("Creating new transaction with ID: {}", transaction.getTransactionId());


        String transactionId = transaction.getTransactionId();
        Transaction existing = transactionStore.putIfAbsent(transactionId, transaction);
        if (existing != null) {
            throw new IllegalStateException("Transaction ID " + transactionId + " already exists");
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
    }

    @Override
    public Transaction getTransactionOrThrow(String id) {
        Transaction transaction = transactionStore.get(id);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", id);
            throw new TransactionNotFoundException("Transaction not found: " + id);
        }
        return transaction;
    }


    @Override
    @Transactional
    public void deleteTransaction(String id) {

        transactionStore.compute(id, (key, existing) -> {
            if (existing == null) {
                logger.warn("Transaction not found: {}", id);
                throw new TransactionNotFoundException("Transaction not found: " + id);
            }
            logger.info("Deleting transaction: {}", id);
            return null; // Returning null removes the entry
        });

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Transaction updateTransactionStatus(String transactionId, TransactionStatus status) {

        Transaction updated = transactionStore.compute(transactionId, (key, existing) -> {
            if (existing == null) {
                logger.warn("Transaction not found: {}", transactionId);
                throw new TransactionNotFoundException("Transaction not found: " + transactionId);
            }

            validateStatusTransition(existing.getStatus(), status);

            // Merge or copy fields as needed
            Transaction newTransaction = Transaction.coloneTransaction(existing,status);

            logger.info("Updating transaction {} status from {} to {}",
                    transactionId, existing.getStatus(), status);

            return newTransaction;
        });

        return updated;
    }




} 