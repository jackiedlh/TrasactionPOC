package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.AccountAlwaysExistException;
import com.hsbc.transaction.exception.AccountNotFoundException;
import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Service
@CacheConfig(cacheNames = "accounts")
public class AccountServiceImpl implements AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final ConcurrentHashMap<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();

    @Override
    @Transactional
    @CachePut(key = "#accountNo")
    public void createAccount(String accountNo, BigDecimal initBalance) {
        if (initBalance == null) {
            throw new IllegalArgumentException("Initial balance cannot be null");
        }
        if (initBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        accountBalances.compute(accountNo, (key, existing) -> {
            if (existing != null) {
                throw new AccountAlwaysExistException("Account already exists: " + accountNo);
            }
            return initBalance;
        });
    }

    @Override
    @Transactional
    @CachePut(key = "#accountNo")
    public void credit(String accountNo, BigDecimal amount) {
        accountBalances.compute(accountNo, (key, currentBalance) -> {
            if (currentBalance == null) {
                throw new AccountNotFoundException("Account not found: " + accountNo);
            }
            return currentBalance.add(amount);
        });
    }

    @Override
    @Transactional
    @CachePut(key = "#accountNo")
    public void debit(String accountNo, BigDecimal amount) {
        accountBalances.compute(accountNo, (key, currentBalance) -> {
            if (currentBalance == null) {
                throw new AccountNotFoundException("Account not found: " + accountNo);
            } else if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                    String.format("Insufficient balance in account %s. Required: %s, Available: %s",
                        accountNo, amount, currentBalance)
                );
            }
            return currentBalance.subtract(amount);
        });
    }

    @Override
    @CachePut(key = "#accountNo")
    public BigDecimal getBalance(String accountNo) {
        BigDecimal balance = accountBalances.get(accountNo);
        if (balance == null) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }
        return balance;
    }

    @Override
    @Transactional
    @CacheEvict(key = "#accountNo")
    public void deleteAccount(String accountNo) {
        accountBalances.compute(accountNo, (key, existing) -> {
            if (existing == null) {
                throw new AccountNotFoundException("Account not found: " + accountNo);
            }
            logger.info("Deleting account: {}", accountNo);
            return null;
        });
    }

    @Transactional
    @Override
    public void updateAccountBalance(Transaction transaction) {
        String accountNo = transaction.getAccountNo();
        BigDecimal amount = transaction.getAmount();

        if (transaction.getDirection() == TransactionDirection.DEBIT) {
            debit(accountNo, amount);
            logger.info("Debited {} from account {}", amount, accountNo);
        } else if (transaction.getDirection() == TransactionDirection.CREDIT) {
            credit(accountNo, amount);
            logger.info("Credited {} to account {}", amount, accountNo);
        }
    }
} 