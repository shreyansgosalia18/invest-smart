package com.blackrock.investsmart.service;

import com.blackrock.investsmart.model.domain.*;
import com.blackrock.investsmart.model.request.ReturnCalculationRequest;
import com.blackrock.investsmart.model.request.TemporalFilterRequest;
import com.blackrock.investsmart.model.request.TransactionValidationRequest;
import com.blackrock.investsmart.model.response.*;
import com.blackrock.investsmart.util.DateTimeParser;
import com.blackrock.investsmart.util.FinancialCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Core business logic — orchestrates the 5-step savings pipeline.
 *
 * <p>Pipeline: Raw Expenses → Parse → Validate → Apply Q → Apply P → Group K → Calculate Returns
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Single service with well-separated methods — this is a single-domain problem,
 *       splitting into multiple services would be fragmentation, not separation of concerns</li>
 *   <li>Validation is fail-fast: negatives and duplicates are removed before q/p processing</li>
 *   <li>Duplicate detection uses HashSet with O(1) lookup — essential for up to 10⁶ transactions</li>
 *   <li>Period matching is O(n × m) where n=transactions, m=periods — acceptable for the
 *       constraints. Could be optimized with interval trees if needed (documented trade-off)</li>
 * </ul>
 */
@Slf4j
@Service
public class TransactionProcessingService {

    // ======================== Endpoint 1: Parse ========================

    /**
     * Enriches raw expenses with ceiling and remanent.
     * Step 1 of the pipeline.
     *
     * @param expenses raw expense list
     * @return list of transactions with ceiling and remanent calculated
     */
    public List<Transaction> parseTransactions(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) return List.of();

        return expenses.stream()
                .map(e -> Transaction.builder()
                        .date(e.getDate())
                        .amount(e.getAmount())
                        .ceiling(FinancialCalculator.ceiling100(e.getAmount()))
                        .remanent(FinancialCalculator.remanent(e.getAmount()))
                        .build())
                .toList();
    }

    // ======================== Endpoint 2: Validate ========================

    /**
     * Validates transactions — separates into valid and invalid.
     * Checks for negative amounts and duplicates (same date + amount).
     *
     * @param request contains wage and pre-parsed transactions
     * @return response with valid and invalid transaction lists
     */
    public TransactionValidationResponse validateTransactions(TransactionValidationRequest request) {
        List<Transaction> transactions = request.getTransactions();
        if (transactions == null || transactions.isEmpty()) {
            return TransactionValidationResponse.builder()
                    .valid(List.of())
                    .invalid(List.of())
                    .build();
        }

        List<Transaction> valid = new ArrayList<>();
        List<InvalidTransaction> invalid = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Transaction t : transactions) {
            // Check negative amounts first
            if (t.getAmount() < 0) {
                invalid.add(InvalidTransaction.builder()
                        .date(t.getDate())
                        .amount(t.getAmount())
                        .message("Negative amounts are not allowed")
                        .build());
                continue;
            }

            // Check duplicates: same date + same amount
            String key = t.getDate() + "|" + t.getAmount();
            if (!seen.add(key)) {
                invalid.add(InvalidTransaction.builder()
                        .date(t.getDate())
                        .amount(t.getAmount())
                        .message("Duplicate transaction")
                        .build());
                continue;
            }

            valid.add(t);
        }

        return TransactionValidationResponse.builder()
                .valid(valid)
                .invalid(invalid)
                .build();
    }

    // ======================== Endpoint 3: Filter ========================

    /**
     * Full temporal filtering pipeline: parse → validate → apply q/p → check k membership.
     *
     * @param request contains q/p/k periods, wage, and raw expenses
     * @return filtered valid transactions (with inkPeriod flag) and invalid transactions
     */
    public TemporalFilterResponse filterTransactions(TemporalFilterRequest request) {
        // Step 1: Parse raw expenses
        List<Transaction> parsed = parseTransactions(request.getTransactions());

        // Step 2: Validate (negatives + duplicates)
        TransactionValidationRequest valRequest = TransactionValidationRequest.builder()
                .wage(request.getWage())
                .transactions(parsed)
                .build();
        TransactionValidationResponse valResponse = validateTransactions(valRequest);

        // Steps 3-4: Apply q and p periods, check k membership
        List<FilteredTransaction> validFiltered = new ArrayList<>();
        for (Transaction t : valResponse.getValid()) {
            double adjustedRemanent = applyTemporalRules(t, request.getQ(), request.getP());
            boolean inK = isInAnyKPeriod(t.getDate(), request.getK());

            validFiltered.add(FilteredTransaction.builder()
                    .date(t.getDate())
                    .amount(t.getAmount())
                    .ceiling(t.getCeiling())
                    .remanent(adjustedRemanent)
                    .inkPeriod(inK)
                    .build());
        }

        return TemporalFilterResponse.builder()
                .valid(validFiltered)
                .invalid(valResponse.getInvalid())
                .build();
    }

    // ======================== Endpoint 4a: NPS Returns ========================

    /**
     * Full pipeline → NPS return calculation per k period.
     *
     * @param request contains age, wage, inflation, q/p/k periods, and raw expenses
     * @return NPS returns with profit and tax benefit per k period
     */
    public NpsReturnResponse calculateNpsReturns(ReturnCalculationRequest request) {
        ProcessedData data = processFullPipeline(request);
        int years = FinancialCalculator.investmentYears(request.getAge());

        List<NpsReturnResponse.NpsSavingsByDate> savingsList = new ArrayList<>();
        for (int i = 0; i < request.getK().size(); i++) {
            KPeriod kp = request.getK().get(i);
            double amount = data.kPeriodSums.get(i);
            double profit = FinancialCalculator.npsProfit(amount, years, request.getInflation());
            double taxBenefit = FinancialCalculator.calculateNpsTaxBenefit(amount, request.getWage());

            savingsList.add(NpsReturnResponse.NpsSavingsByDate.builder()
                    .start(kp.getStart())
                    .end(kp.getEnd())
                    .amount(amount)
                    .profit(profit)
                    .taxBenefit(taxBenefit)
                    .build());
        }

        return NpsReturnResponse.builder()
                .totalTransactionAmount(data.totalAmount)
                .totalCeiling(data.totalCeiling)
                .savingsByDates(savingsList)
                .build();
    }

    // ======================== Endpoint 4b: Index Returns ========================

    /**
     * Full pipeline → NIFTY 50 Index Fund return calculation per k period.
     *
     * @param request contains age, wage, inflation, q/p/k periods, and raw expenses
     * @return Index returns with total real value per k period
     */
    public IndexReturnResponse calculateIndexReturns(ReturnCalculationRequest request) {
        ProcessedData data = processFullPipeline(request);
        int years = FinancialCalculator.investmentYears(request.getAge());

        List<IndexReturnResponse.IndexSavingsByDate> savingsList = new ArrayList<>();
        for (int i = 0; i < request.getK().size(); i++) {
            KPeriod kp = request.getK().get(i);
            double amount = data.kPeriodSums.get(i);
            double returnVal = FinancialCalculator.indexReturn(amount, years, request.getInflation());

            savingsList.add(IndexReturnResponse.IndexSavingsByDate.builder()
                    .start(kp.getStart())
                    .end(kp.getEnd())
                    .amount(amount)
                    .returnValue(returnVal)
                    .taxBenefit(0.0)
                    .build());
        }

        return IndexReturnResponse.builder()
                .totalTransactionAmount(data.totalAmount)
                .totalCeiling(data.totalCeiling)
                .savingsByDates(savingsList)
                .build();
    }

    // ======================== Internal Pipeline ========================

    /**
     * Runs the full pipeline (parse → validate → q/p → k-grouping) and returns
     * intermediate results used by both NPS and Index endpoints.
     */
    private ProcessedData processFullPipeline(ReturnCalculationRequest request) {
        // Step 1: Parse
        List<Transaction> parsed = parseTransactions(request.getTransactions());

        // Step 2: Validate
        TransactionValidationRequest valRequest = TransactionValidationRequest.builder()
                .wage(request.getWage())
                .transactions(parsed)
                .build();
        TransactionValidationResponse valResponse = validateTransactions(valRequest);
        List<Transaction> validTransactions = valResponse.getValid();

        // Calculate totals from valid transactions
        double totalAmount = validTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalCeiling = validTransactions.stream().mapToDouble(Transaction::getCeiling).sum();

        // Steps 3-4: Apply q/p and compute adjusted remanents
        List<AdjustedTransaction> adjusted = new ArrayList<>();
        for (Transaction t : validTransactions) {
            double adjustedRemanent = applyTemporalRules(t, request.getQ(), request.getP());
            adjusted.add(new AdjustedTransaction(t.getDate(), adjustedRemanent));
        }

        // Step 5: Group by k periods — sum adjusted remanents per k period
        List<Double> kPeriodSums = new ArrayList<>();
        for (KPeriod kp : request.getK()) {
            LocalDateTime kStart = DateTimeParser.parse(kp.getStart());
            LocalDateTime kEnd = DateTimeParser.parse(kp.getEnd());

            double sum = 0;
            for (AdjustedTransaction at : adjusted) {
                LocalDateTime txDate = DateTimeParser.parse(at.date);
                if (DateTimeParser.isWithinRange(txDate, kStart, kEnd)) {
                    sum += at.remanent;
                }
            }
            kPeriodSums.add(FinancialCalculator.round2(sum));
        }

        return new ProcessedData(totalAmount, totalCeiling, kPeriodSums);
    }

    // ======================== Q Period Logic ========================

    /**
     * Applies q period rules then p period rules to a transaction's remanent.
     *
     * <p>Q rules (Step 2): Replace remanent with fixed amount.
     * If multiple q periods match → latest start date wins → first-in-list breaks ties.
     *
     * <p>P rules (Step 3): Add extra to remanent. All matching p periods stack.
     * Applied AFTER q — so q replaces first, then p adds on top.
     *
     * @param transaction the parsed transaction
     * @param qPeriods list of q period definitions
     * @param pPeriods list of p period definitions
     * @return final adjusted remanent
     */
    private double applyTemporalRules(Transaction transaction, List<QPeriod> qPeriods, List<PPeriod> pPeriods) {
        LocalDateTime txDate = DateTimeParser.parse(transaction.getDate());
        double remanent = transaction.getRemanent();

        // Step 2: Apply q periods — find the winning q period (if any)
        remanent = applyQPeriods(txDate, remanent, qPeriods);

        // Step 3: Apply p periods — stack all matching extras
        remanent = applyPPeriods(txDate, remanent, pPeriods);

        return remanent;
    }

    /**
     * Q period resolution: latest start date wins, first-in-list breaks ties.
     */
    private double applyQPeriods(LocalDateTime txDate, double remanent, List<QPeriod> qPeriods) {
        if (qPeriods == null || qPeriods.isEmpty()) return remanent;

        QPeriod winner = null;
        LocalDateTime winnerStart = null;

        for (QPeriod qp : qPeriods) {
            LocalDateTime qStart = DateTimeParser.parse(qp.getStart());
            LocalDateTime qEnd = DateTimeParser.parse(qp.getEnd());

            if (DateTimeParser.isWithinRange(txDate, qStart, qEnd)) {
                if (winner == null) {
                    // First matching q period
                    winner = qp;
                    winnerStart = qStart;
                } else if (qStart.isAfter(winnerStart)) {
                    // This q period starts later → it wins
                    winner = qp;
                    winnerStart = qStart;
                }
                // If same start date → first-in-list wins (we keep existing winner)
            }
        }

        return (winner != null) ? winner.getFixed() : remanent;
    }

    /**
     * P period resolution: all matching periods stack (sum all extras).
     */
    private double applyPPeriods(LocalDateTime txDate, double remanent, List<PPeriod> pPeriods) {
        if (pPeriods == null || pPeriods.isEmpty()) return remanent;

        double totalExtra = 0;
        for (PPeriod pp : pPeriods) {
            LocalDateTime pStart = DateTimeParser.parse(pp.getStart());
            LocalDateTime pEnd = DateTimeParser.parse(pp.getEnd());

            if (DateTimeParser.isWithinRange(txDate, pStart, pEnd)) {
                totalExtra += pp.getExtra();
            }
        }

        return remanent + totalExtra;
    }

    // ======================== K Period Check ========================

    /**
     * Checks if a transaction date falls within any k period.
     * Used by the filter endpoint to set the {@code inkPeriod} flag.
     */
    private boolean isInAnyKPeriod(String dateStr, List<KPeriod> kPeriods) {
        if (kPeriods == null || kPeriods.isEmpty()) return false;

        LocalDateTime txDate = DateTimeParser.parse(dateStr);
        for (KPeriod kp : kPeriods) {
            if (DateTimeParser.isWithinRange(txDate, kp.getStart(), kp.getEnd())) {
                return true;
            }
        }
        return false;
    }

    // ======================== Internal Data Holders ========================

    /** Intermediate result from the full pipeline — used by both NPS and Index endpoints */
    private record ProcessedData(double totalAmount, double totalCeiling, List<Double> kPeriodSums) {}

    /** Transaction with adjusted remanent after q/p rules */
    private record AdjustedTransaction(String date, double remanent) {}
}
