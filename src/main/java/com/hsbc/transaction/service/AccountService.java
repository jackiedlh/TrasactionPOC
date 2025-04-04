package com.hsbc.transaction.service;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

import com.hsbc.transaction.model.Transaction;

public interface AccountService {

    BigDecimal getBalance(String accountNo);
    void credit(String accountNo, BigDecimal amount);

    @Transactional
    void createAccount(String accountNo, BigDecimal initBalance);

    void debit(String accountNo, BigDecimal amount);

    void updateAccountBalance(Transaction transaction);
}