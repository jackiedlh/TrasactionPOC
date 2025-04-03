package com.hsbc.transaction.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionFilter {
    private String accountNo;
    private TransactionDirection direction;
    private TransactionStatus status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
} 