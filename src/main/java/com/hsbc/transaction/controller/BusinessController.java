package com.hsbc.transaction.controller;

import com.hsbc.transaction.model.TransferRequest;
import com.hsbc.transaction.service.BusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Tag(name = "Business Operations", description = "APIs for business operations like transfers")
public class BusinessController {
    private final BusinessService businessService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer amount between accounts", description = "Transfer money from one account to another")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        businessService.transfer(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                request.getDescription()
        );
        return ResponseEntity.ok().build();
    }
} 