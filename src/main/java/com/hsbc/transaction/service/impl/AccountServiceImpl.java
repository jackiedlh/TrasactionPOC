package com.hsbc.transaction.service.impl;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.exception.AccountAlwaysExistException;
import com.hsbc.transaction.exception.AccountNotFoundException;
import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.service.AccountService;

@Service
public class AccountServiceImpl implements AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final ConcurrentHashMap<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void credit(String accountNo, BigDecimal amount) {

            //BigDecimal currentBalance = getBalance(accountNo);
            accountBalances.compute(accountNo, (key, currentBalance) -> {
                if (currentBalance == null) {
                    throw new AccountNotFoundException("Account not found: " + accountNo);
                }
                return currentBalance.add(amount);
            });

    }

    @Transactional
    @Override
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
    public void debit(String accountNo, BigDecimal amount) {

        accountBalances.compute(accountNo, (key, currentBalance) -> {
            if (currentBalance == null) {
                throw new AccountNotFoundException("Account not found: " + accountNo);
            }else if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance in account %s. Required: %s, Available: %s",
                                accountNo, amount, currentBalance)
                );
            }
            return currentBalance.subtract(amount);
        });
    }

    @Override
    public BigDecimal getBalance(String accountNo) {
        BigDecimal balance = accountBalances.get(accountNo);
        if (balance == null) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }
        return balance;
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