package com.blackrock.investsmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A q period — overrides the remanent with a fixed amount for transactions within [start, end].
 *
 * <p>Conflict resolution: if multiple q periods match a transaction,
 * the one with the latest start date wins. Tie-break: first in the input list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QPeriod {

    /** Fixed remanent override amount */
    private double fixed;

    /** Period start (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String start;

    /** Period end (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String end;
}
