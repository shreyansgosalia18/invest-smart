package com.blackrock.investsmart.model.request;

import com.blackrock.investsmart.model.Expense;
import com.blackrock.investsmart.model.KPeriod;
import com.blackrock.investsmart.model.PPeriod;
import com.blackrock.investsmart.model.QPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the Returns Calculation endpoints (NPS and Index).
 * Contains all data needed for the full pipeline: parse → validate → q/p → k → invest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCalculationRequest {

    /** Investor's current age */
    private int age;

    /** Monthly wage in Indian Rupees */
    private double wage;

    /** Annual inflation rate as percentage (e.g., 5.5 for 5.5%) */
    private double inflation;

    /** q periods — fixed remanent overrides */
    private List<QPeriod> q;

    /** p periods — extra savings additions */
    private List<PPeriod> p;

    /** k periods — evaluation grouping ranges */
    private List<KPeriod> k;

    /** Raw expense list (date + amount only) */
    private List<Expense> transactions;
}
