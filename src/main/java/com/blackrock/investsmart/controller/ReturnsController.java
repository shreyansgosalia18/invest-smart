package com.blackrock.investsmart.controller;

import com.blackrock.investsmart.model.request.ReturnCalculationRequest;
import com.blackrock.investsmart.model.response.IndexReturnResponse;
import com.blackrock.investsmart.model.response.NpsReturnResponse;
import com.blackrock.investsmart.model.response.SavingsOptimizationResponse;
import com.blackrock.investsmart.service.SavingsOptimizationService;
import com.blackrock.investsmart.service.TransactionProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Investment return endpoints — NPS and NIFTY 50 Index Fund calculations.
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Tag(name = "Returns", description = "Calculate investment returns with inflation adjustment")
public class ReturnsController {

    private final TransactionProcessingService processingService;
    private final SavingsOptimizationService optimizationService;

    @PostMapping("/returns:nps")
    @Operation(summary = "Calculate NPS returns with profit and tax benefit per k period")
    public ResponseEntity<NpsReturnResponse> calculateNpsReturns(
            @RequestBody ReturnCalculationRequest request) {
        return ResponseEntity.ok(processingService.calculateNpsReturns(request));
    }

    @PostMapping("/returns:index")
    @Operation(summary = "Calculate NIFTY 50 Index Fund returns per k period")
    public ResponseEntity<IndexReturnResponse> calculateIndexReturns(
            @RequestBody ReturnCalculationRequest request) {
        return ResponseEntity.ok(processingService.calculateIndexReturns(request));
    }

    @PostMapping("/optimize")
    @Operation(summary = "Analyze savings and get actionable optimization recommendations")
    public ResponseEntity<SavingsOptimizationResponse> optimizeSavings(
            @RequestBody ReturnCalculationRequest request) {
        return ResponseEntity.ok(optimizationService.optimize(request));
    }
}
