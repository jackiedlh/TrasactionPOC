package com.hsbc.transaction.controller;

import com.hsbc.transaction.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Operations", description = "APIs for account operations")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountNo}/balance")
    @Operation(summary = "Get account balance", description = "Retrieve the current balance of an account")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNo) {
        return ResponseEntity.ok(accountService.getBalance(accountNo));
    }

    @PostMapping
    @Operation(summary = "Create account", description = "Create a new account with initial balance")
    public ResponseEntity<Void> createAccount(
            @RequestParam String accountNo, 
            @RequestParam BigDecimal initialBalance) {
        accountService.createAccount(accountNo, initialBalance);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{accountNo}")
    @Operation(summary = "Delete account", description = "Delete an existing account")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountNo) {
        accountService.deleteAccount(accountNo);
        return ResponseEntity.ok().build();
    }
} 