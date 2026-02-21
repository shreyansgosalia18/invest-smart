package com.blackrock.investsmart.model.response;

import com.blackrock.investsmart.model.domain.InvalidTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Temporal Constraints Filter endpoint (endpoint 3).
 * Valid transactions include the {@code inkPeriod} flag; invalid ones have error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalFilterResponse {

    /** Transactions that passed validation, with q/p adjustments and k-period flags */
    private List<FilteredTransaction> valid;

    /** Transactions that failed validation (negatives, duplicates) */
    private List<InvalidTransaction> invalid;
}
