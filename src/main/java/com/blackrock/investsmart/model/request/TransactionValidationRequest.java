package com.blackrock.investsmart.model.request;

import com.blackrock.investsmart.model.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the Transaction Validator endpoint (endpoint 2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationRequest {

    /** Monthly wage in Indian Rupees */
    private double wage;

    /** Pre-parsed transactions (with ceiling and remanent already calculated) */
    private List<Transaction> transactions;
}
