package com.hsbc.transaction.service;

import com.hsbc.transaction.model.Transaction;

import java.util.List;

public interface BusinessService {
    void combine(List<Transaction> transactions);


}