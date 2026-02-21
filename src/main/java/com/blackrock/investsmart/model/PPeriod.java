package com.blackrock.investsmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A p period — adds an extra amount to the remanent for transactions within [start, end].
 *
 * <p>Multiple p periods stack: if a transaction falls in 3 overlapping p periods,
 * all three extras are summed and added to the remanent.
 * p is applied AFTER q — so q replaces first, then p adds on top.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PPeriod {

    /** Extra amount to add to each matching transaction's remanent */
    private double extra;

    /** Period start (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String start;

    /** Period end (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String end;
}
