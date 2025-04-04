package com.hsbc.transaction.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Transaction details")
public class Transaction {
    @Schema(description = "Unique transaction ID")
    private String transactionId;

    @NotBlank(message = "Account number is required")
    @Schema(description = "Account number")
    private String accountNo;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Schema(description = "Transaction amount")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Schema(description = "Transaction description")
    private String description;

    @NotNull(message = "Transaction direction is required")
    @Schema(description = "Transaction direction (IN/OUT)")
    private TransactionDirection direction;

    @Schema(description = "Transaction status")
    private TransactionStatus status;

    @Schema(description = "Transaction timestamp")
    private LocalDateTime timestamp;

    public static Transaction createInitialTransaction(String accountNo, BigDecimal amount, String description, TransactionDirection direction) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo(accountNo)
                .amount(amount)
                .description(description)
                .direction(direction)
                .status(TransactionStatus.RUNNING)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static Transaction coloneTransaction(Transaction transaction) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo(transaction.accountNo)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .direction(transaction.getDirection())
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .build();
    }

    public static Transaction revertTransaction(Transaction transaction) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNo(transaction.accountNo)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .direction(transaction.getDirection()==TransactionDirection.DEBIT?TransactionDirection.CREDIT:TransactionDirection.DEBIT)
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .build();
    }
} 