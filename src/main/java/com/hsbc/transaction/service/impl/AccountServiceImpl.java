package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.exception.InsufficientBalanceException;
import com.hsbc.transaction.service.AccountService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountServiceImpl implements AccountService {
    private Map<String, BigDecimal> accountBalances;

    public Map<String, BigDecimal> getAccountBalances() {
        return accountBalances;
    }

    public void setAccountBalances(Map<String, BigDecimal> accountBalances) {
        this.accountBalances = accountBalances;
    }



    public AccountServiceImpl() {
        this.accountBalances = new ConcurrentHashMap<>();
    }

    public AccountServiceImpl(Map<String, BigDecimal> balances) {
        this.accountBalances = balances;
    }

    @Override
    public BigDecimal getBalance(String accountNo) {
        return accountBalances.getOrDefault(accountNo, BigDecimal.ZERO);
    }

    @Override
    public void setBalance(String accountNo, BigDecimal amount) {
        if (accountBalances == null) {
            accountBalances = new ConcurrentHashMap<>();
        }
        accountBalances.put(accountNo, amount);
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