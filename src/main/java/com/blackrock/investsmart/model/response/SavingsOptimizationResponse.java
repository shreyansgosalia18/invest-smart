package com.blackrock.investsmart.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Savings Optimizer innovation endpoint.
 * Provides actionable insights beyond simple return calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsOptimizationResponse {

    /** Total remanent before any q/p adjustments */
    private double totalRawSavings;

    /** Amount lost due to q period constraints (negative impact) */
    private double qPeriodImpact;

    /** Amount gained from p period additions (positive impact) */
    private double pPeriodBoost;

    /** Years until retirement */
    private int investmentYears;

    /** NPS vs Index comparison per k period */
    private List<InstrumentComparison> comparisons;

    /** Overall actionable advice */
    private String overallAdvice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentComparison {
        private String start;
        private String end;
        private double investedAmount;
        private double npsProfit;
        private double npsTaxBenefit;
        private double indexReturn;
        private String recommendation;
    }
}
