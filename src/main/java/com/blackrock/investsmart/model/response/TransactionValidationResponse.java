package com.blackrock.investsmart.model.response;

import com.blackrock.investsmart.model.domain.InvalidTransaction;
import com.blackrock.investsmart.model.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Transaction Validator endpoint (endpoint 2).
 * Separates transactions into valid and invalid lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationResponse {

    /** Transactions that passed validation */
    private List<Transaction> valid;

    /** Transactions that failed validation, with error messages */
    private List<InvalidTransaction> invalid;
}
