package com.blackrock.investsmart.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A valid transaction after temporal filtering (endpoint 3).
 * Extends the base transaction fields with {@code inkPeriod} to indicate
 * whether this transaction falls within any k evaluation period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteredTransaction {

    /** Timestamp in "YYYY-MM-DD HH:mm:ss" format */
    private String date;

    /** Original expense amount */
    private double amount;

    /** Next multiple of 100 */
    private double ceiling;

    /** Final remanent after q and p period adjustments */
    private double remanent;

    /** True if this transaction falls within at least one k period */
    private boolean inkPeriod;
}
