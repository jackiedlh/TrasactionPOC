package com.hsbc.transaction.service;

import java.math.BigDecimal;

public interface BusinessService {
    void transfer(String fromAccount, String toAccount, BigDecimal amount, String description);
} 