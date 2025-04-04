package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface BusinessService {
    @Transactional
    void combine(List<Transaction> transactions);


}