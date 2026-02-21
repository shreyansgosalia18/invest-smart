package com.blackrock.investsmart.model.request;

import com.blackrock.investsmart.model.domain.Expense;
import com.blackrock.investsmart.model.domain.KPeriod;
import com.blackrock.investsmart.model.domain.PPeriod;
import com.blackrock.investsmart.model.domain.QPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the Temporal Constraints Filter endpoint (endpoint 3).
 * Contains raw expenses and all temporal period definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalFilterRequest {

    /** q periods — fixed remanent overrides */
    private List<QPeriod> q;

    /** p periods — extra savings additions */
    private List<PPeriod> p;

    /** k periods — evaluation grouping ranges */
    private List<KPeriod> k;

    /** Monthly wage in Indian Rupees */
    private double wage;

    /** Raw expense list (date + amount only — ceiling/remanent computed internally) */
    private List<Expense> transactions;
}
