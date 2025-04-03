package com.hsbc.transaction.controller;

import com.hsbc.transaction.service.AccountService;
import com.hsbc.transaction.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    private final TransferService transferService;

    public AccountController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @GetMapping("/{accountNo}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNo) {
        return ResponseEntity.ok(accountService.getBalance(accountNo));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        transferService.transfer(
            request.getFromAccount(),
            request.getToAccount(),
            request.getAmount(),
            request.getDescription()
        );
        return ResponseEntity.ok().build();
    }
}

@Data
class TransferRequest {
    @NotBlank(message = "Source account is required")
    private String fromAccount;

    @NotBlank(message = "Destination account is required")
    private String toAccount;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;
} 