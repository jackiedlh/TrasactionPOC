package com.hsbc.transaction.service;

import java.math.BigDecimal;

public interface AccountService {
    BigDecimal getBalance(String accountNo);
    void credit(String accountNo, BigDecimal amount);
    void debit(String accountNo, BigDecimal amount);
} 