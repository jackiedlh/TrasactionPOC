package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;

import java.math.BigDecimal;

public interface AccountService {

    BigDecimal getBalance(String accountNo);
    void credit(String accountNo, BigDecimal amount);

    void createAccount(String accountNo, BigDecimal initBalance);

    void debit(String accountNo, BigDecimal amount);

    boolean exist(String accountNo);

    void updateAccountBalance(Transaction transaction);

    void deleteAccount(String accountNo);
}