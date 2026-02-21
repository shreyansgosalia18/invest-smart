package com.blackrock.investsmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A transaction that failed validation, with an explanation of why.
 * Used in both validator and filter endpoint responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidTransaction {

    /** Timestamp of the rejected transaction */
    private String date;

    /** Original amount */
    private double amount;

    /** Human-readable reason for rejection (e.g., "Negative amounts are not allowed") */
    private String message;
}
