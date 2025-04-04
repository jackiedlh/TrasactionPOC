package com.hsbc.transaction.controller;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionStatus;
import com.hsbc.transaction.model.TransactionDirection;
import com.hsbc.transaction.model.PageResponse;
import com.hsbc.transaction.model.TransactionFilter;
import com.hsbc.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Controller", description = "APIs for managing transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.createTransaction(transaction));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update transaction status")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable String id,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, status));
    }



    @GetMapping
    @Operation(summary = "Query transactions with optional filters and pagination")
    public ResponseEntity<PageResponse<Transaction>> queryTransactions(
            @Parameter(description = "Account number to filter by")
            @RequestParam(required = false) String accountNo,
            @Parameter(description = "Transaction direction to filter by")
            @RequestParam(required = false) TransactionDirection direction,
            @Parameter(description = "Transaction status to filter by")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Minimum amount to filter by")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum amount to filter by")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Start date to filter by")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "End date to filter by")
            @RequestParam(required = false) LocalDateTime toDate,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        
        TransactionFilter filter = TransactionFilter.builder()
                .accountNo(accountNo)
                .direction(direction)
                .status(status)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        return ResponseEntity.ok(transactionService.queryTransactions(filter, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }
} 