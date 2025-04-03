package com.hsbc.transaction.service;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountServiceImpl implements AccountService {
    private final Map<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getBalance(String accountNo) {
        return accountBalances.getOrDefault(accountNo, BigDecimal.ZERO);
    }

    @Override
    public synchronized void credit(String accountNo, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(accountNo);
        BigDecimal newBalance = currentBalance.add(amount);
        accountBalances.put(accountNo, newBalance);
    }

    @Override
    public synchronized void debit(String accountNo, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(accountNo);
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance in account %s. Required: %s, Available: %s",
                    accountNo, amount, currentBalance)
            );
        }
        BigDecimal newBalance = currentBalance.subtract(amount);
        accountBalances.put(accountNo, newBalance);
    }
} 