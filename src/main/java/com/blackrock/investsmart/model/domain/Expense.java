package com.blackrock.investsmart.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Raw expense input — a single purchase made by the user.
 * This is the starting point of the pipeline before any enrichment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    /** Timestamp of the expense in "YYYY-MM-DD HH:mm:ss" format */
    private String date;

    /** Expense amount in Indian Rupees */
    private double amount;
}
