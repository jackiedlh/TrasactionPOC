package com.hsbc.transaction.service;

import java.math.BigDecimal;
import java.util.Map;

public interface AccountService {
    Map<String, BigDecimal> getAccountBalances();


    void setAccountBalances(Map<String, BigDecimal> accountBalances);

    BigDecimal getBalance(String accountNo);
    void setBalance(String accountNo, BigDecimal amount);
    void credit(String accountNo, BigDecimal amount);
    void debit(String accountNo, BigDecimal amount);
} 