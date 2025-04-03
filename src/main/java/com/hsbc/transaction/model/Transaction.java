package com.hsbc.transaction.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction entity representing a financial transaction")
public class Transaction {
    @Schema(description = "Unique identifier of the transaction", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Account number associated with the transaction", example = "1234567890", required = true)
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{8,12}$", message = "Account number must be between 8 and 12 digits")
    private String accountNo;

    @Schema(description = "Transaction amount", example = "100.50", required = true)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Schema(description = "Transaction description", example = "Payment for services", required = true)
    @NotNull(message = "Description is required")
    private String description;

    @Schema(description = "Transaction direction (IN for incoming, OUT for outgoing)", example = "IN")
    @NotNull(message = "Direction is required")
    private TransactionDirection direction;

    @Schema(description = "Transaction timestamp", example = "2024-04-03T10:15:30")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Transaction status", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "FAILED", "CANCELLED"})
    private String status;
} 