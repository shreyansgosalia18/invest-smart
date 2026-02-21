package com.blackrock.investsmart.controller;

import com.blackrock.investsmart.model.domain.Expense;
import com.blackrock.investsmart.model.domain.Transaction;
import com.blackrock.investsmart.model.request.TemporalFilterRequest;
import com.blackrock.investsmart.model.request.TransactionValidationRequest;
import com.blackrock.investsmart.model.response.TemporalFilterResponse;
import com.blackrock.investsmart.model.response.TransactionValidationResponse;
import com.blackrock.investsmart.service.TransactionProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction endpoints — parsing, validation, and temporal filtering.
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Parse, validate, and filter financial transactions")
public class TransactionController {

    private final TransactionProcessingService processingService;

    @PostMapping("/transactions:parse")
    @Operation(summary = "Parse expenses into transactions with ceiling and remanent")
    public ResponseEntity<List<Transaction>> parseTransactions(@RequestBody List<Expense> expenses) {
        return ResponseEntity.ok(processingService.parseTransactions(expenses));
    }

    @PostMapping("/transactions:validator")
    @Operation(summary = "Validate transactions — detect negatives and duplicates")
    public ResponseEntity<TransactionValidationResponse> validateTransactions(
            @RequestBody TransactionValidationRequest request) {
        return ResponseEntity.ok(processingService.validateTransactions(request));
    }

    @PostMapping("/transactions:filter")
    @Operation(summary = "Apply temporal constraints (q/p/k periods) and filter transactions")
    public ResponseEntity<TemporalFilterResponse> filterTransactions(
            @RequestBody TemporalFilterRequest request) {
        return ResponseEntity.ok(processingService.filterTransactions(request));
    }
}
