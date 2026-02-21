package com.blackrock.investsmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A k period — defines a date range for grouping and summing remanents.
 * Each k period independently sums all matching transactions' remanents.
 * A transaction can belong to multiple k periods.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KPeriod {

    /** Period start (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String start;

    /** Period end (inclusive), format: "YYYY-MM-DD HH:mm:ss" */
    private String end;
}
