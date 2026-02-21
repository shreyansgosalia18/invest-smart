package com.blackrock.investsmart.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the NPS Returns endpoint (endpoint 4a).
 *
 * <p>NPS returns include:
 * <ul>
 *   <li>{@code profit} — inflation-adjusted value minus principal (A_real - invested)</li>
 *   <li>{@code taxBenefit} — tax savings from NPS deduction (returned separately, not added to profit)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpsReturnResponse {

    /** Sum of all valid transaction amounts */
    private double totalTransactionAmount;

    /** Sum of all valid transaction ceilings */
    private double totalCeiling;

    /** Per-k-period savings breakdown (same size as k input list) */
    private List<NpsSavingsByDate> savingsByDates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpsSavingsByDate {

        /** k period start timestamp */
        private String start;

        /** k period end timestamp */
        private String end;

        /** Total remanent invested in this k period (after q/p adjustments) */
        private double amount;

        /** Real profit: inflation-adjusted return minus principal */
        private double profit;

        /** Tax benefit from NPS deduction (0 if income falls in 0% slab) */
        private double taxBenefit;
    }
}
