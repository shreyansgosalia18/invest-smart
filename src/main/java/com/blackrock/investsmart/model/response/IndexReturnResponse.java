package com.blackrock.investsmart.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Index Fund Returns endpoint (endpoint 4b).
 *
 * <p>Index fund returns include:
 * <ul>
 *   <li>{@code return} — total inflation-adjusted value (A_real), NOT just profit</li>
 *   <li>No tax benefit (always 0 for index funds)</li>
 * </ul>
 *
 * <p>Note: the JSON field name is "return" (a Java reserved word),
 * so we use {@code @JsonProperty} to map it correctly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexReturnResponse {

    /** Sum of all valid transaction amounts */
    private double totalTransactionAmount;

    /** Sum of all valid transaction ceilings */
    private double totalCeiling;

    /** Per-k-period savings breakdown (same size as k input list) */
    private List<IndexSavingsByDate> savingsByDates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexSavingsByDate {

        /** k period start timestamp */
        private String start;

        /** k period end timestamp */
        private String end;

        /** Total remanent invested in this k period (after q/p adjustments) */
        private double amount;

        /** Total inflation-adjusted return value (A_real) — NOT profit */
        @JsonProperty("return")
        private double returnValue;

        /** Tax benefit — always 0 for index funds */
        private double taxBenefit;
    }
}
