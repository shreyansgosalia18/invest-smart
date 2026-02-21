package com.blackrock.investsmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enriched transaction — an expense after Step 1 (ceiling + remanent calculation).
 * Used as output for the parse endpoint and input for the validator endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /** Timestamp in "YYYY-MM-DD HH:mm:ss" format */
    private String date;

    /** Original expense amount */
    private double amount;

    /** Next multiple of 100 above the amount */
    private double ceiling;

    /** Spare change: ceiling - amount */
    private double remanent;
}
