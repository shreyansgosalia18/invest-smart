package com.blackrock.investsmart.service;

// Test type: Unit
// Validation: Verifies full pipeline logic — parse, validate, q/p/k rules, returns calculation
// Command: mvn test -Dtest=TransactionProcessingServiceTest

import com.blackrock.investsmart.model.domain.*;
import com.blackrock.investsmart.model.request.ReturnCalculationRequest;
import com.blackrock.investsmart.model.request.TemporalFilterRequest;
import com.blackrock.investsmart.model.request.TransactionValidationRequest;
import com.blackrock.investsmart.model.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessingServiceTest {

    private TransactionProcessingService service;

    @BeforeEach
    void setUp() {
        service = new TransactionProcessingService();
    }

    // ======================== Endpoint 1: Parse ========================

    @Nested
    @DisplayName("parseTransactions — Step 1: ceiling + remanent")
    class ParseTests {

        @Test
        @DisplayName("Problem statement: 4 expenses parsed correctly")
        void parseFourExpenses() {
            List<Expense> expenses = List.of(
                    new Expense("2023-10-12 20:15:30", 250),
                    new Expense("2023-02-28 15:49:20", 375),
                    new Expense("2023-07-01 21:59:00", 620),
                    new Expense("2023-12-17 08:09:45", 480)
            );

            List<Transaction> result = service.parseTransactions(expenses);

            assertEquals(4, result.size());
            assertEquals(300, result.get(0).getCeiling());
            assertEquals(50, result.get(0).getRemanent());
            assertEquals(400, result.get(1).getCeiling());
            assertEquals(25, result.get(1).getRemanent());
            assertEquals(700, result.get(2).getCeiling());
            assertEquals(80, result.get(2).getRemanent());
            assertEquals(500, result.get(3).getCeiling());
            assertEquals(20, result.get(3).getRemanent());
        }

        @Test
        @DisplayName("Empty list returns empty")
        void emptyInput() {
            assertEquals(List.of(), service.parseTransactions(List.of()));
        }

        @Test
        @DisplayName("Null input returns empty")
        void nullInput() {
            assertEquals(List.of(), service.parseTransactions(null));
        }
    }

    // ======================== Endpoint 2: Validate ========================

    @Nested
    @DisplayName("validateTransactions — negatives and duplicates")
    class ValidateTests {

        @Test
        @DisplayName("Negative amounts are rejected")
        void rejectsNegatives() {
            TransactionValidationRequest req = TransactionValidationRequest.builder()
                    .wage(50000)
                    .transactions(List.of(
                            new Transaction("2023-07-10 09:15:00", -250, 200, 30)
                    ))
                    .build();

            TransactionValidationResponse res = service.validateTransactions(req);
            assertEquals(0, res.getValid().size());
            assertEquals(1, res.getInvalid().size());
            assertEquals("Negative amounts are not allowed", res.getInvalid().get(0).getMessage());
        }

        @Test
        @DisplayName("Duplicate date+amount detected")
        void detectsDuplicates() {
            TransactionValidationRequest req = TransactionValidationRequest.builder()
                    .wage(50000)
                    .transactions(List.of(
                            new Transaction("2023-10-12 20:15:30", 250, 300, 50),
                            new Transaction("2023-10-12 20:15:30", 250, 300, 50)
                    ))
                    .build();

            TransactionValidationResponse res = service.validateTransactions(req);
            assertEquals(1, res.getValid().size());
            assertEquals(1, res.getInvalid().size());
            assertEquals("Duplicate transaction", res.getInvalid().get(0).getMessage());
        }

        @Test
        @DisplayName("Problem statement validator example: 3 valid, 1 invalid (negative)")
        void problemStatementValidatorExample() {
            TransactionValidationRequest req = TransactionValidationRequest.builder()
                    .wage(50000)
                    .transactions(List.of(
                            new Transaction("2023-01-15 10:30:00", 2000, 300, 50),
                            new Transaction("2023-03-20 14:45:00", 3500, 400, 70),
                            new Transaction("2023-06-10 09:15:00", 1500, 200, 30),
                            new Transaction("2023-07-10 09:15:00", -250, 200, 30)
                    ))
                    .build();

            TransactionValidationResponse res = service.validateTransactions(req);
            assertEquals(3, res.getValid().size());
            assertEquals(1, res.getInvalid().size());
        }
    }

    // ======================== Endpoint 3: Filter ========================

    @Nested
    @DisplayName("filterTransactions — full temporal filtering")
    class FilterTests {

        @Test
        @DisplayName("Problem statement filter example: q=0 for Jul, p=30 for Oct-Dec")
        void problemStatementFilterExample() {
            TemporalFilterRequest req = TemporalFilterRequest.builder()
                    .q(List.of(new QPeriod(0, "2023-07-01 00:00:00", "2023-07-31 23:59:59")))
                    .p(List.of(new PPeriod(30, "2023-10-01 00:00:00", "2023-12-31 23:59:59")))
                    .k(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")))
                    .wage(50000)
                    .transactions(List.of(
                            new Expense("2023-02-28 15:49:20", 375),
                            new Expense("2023-07-15 10:30:00", 620),
                            new Expense("2023-10-12 20:15:30", 250),
                            new Expense("2023-10-12 20:15:30", 250),  // duplicate
                            new Expense("2023-12-17 08:09:45", -480)  // negative
                    ))
                    .build();

            TemporalFilterResponse res = service.filterTransactions(req);

            // 2 valid: Feb 28 (375) and Jul 15 (620) and Oct 12 (250)
            // Wait: duplicate Oct 12 → only first kept, negative Dec → rejected
            assertEquals(3, res.getValid().size());
            assertEquals(2, res.getInvalid().size());

            // Feb 28: no q/p applies, remanent = 25
            FilteredTransaction feb = res.getValid().get(0);
            assertEquals(25.0, feb.getRemanent());
            assertTrue(feb.isInkPeriod());

            // Jul 15: q sets remanent to 0
            FilteredTransaction jul = res.getValid().get(1);
            assertEquals(0.0, jul.getRemanent());
            assertTrue(jul.isInkPeriod());

            // Oct 12: remanent = 50 + 30 (p) = 80
            FilteredTransaction oct = res.getValid().get(2);
            assertEquals(80.0, oct.getRemanent());
            assertTrue(oct.isInkPeriod());
        }

        @Test
        @DisplayName("Q period: latest start date wins when multiple match")
        void qPeriodLatestStartWins() {
            TemporalFilterRequest req = TemporalFilterRequest.builder()
                    .q(List.of(
                            new QPeriod(100, "2023-06-01 00:00:00", "2023-08-31 23:59:59"),
                            new QPeriod(200, "2023-07-01 00:00:00", "2023-07-31 23:59:59")
                    ))
                    .p(List.of())
                    .k(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")))
                    .wage(50000)
                    .transactions(List.of(new Expense("2023-07-15 12:00:00", 250)))
                    .build();

            TemporalFilterResponse res = service.filterTransactions(req);
            // Both q periods match Jul 15, but second one starts later (Jul 1 > Jun 1)
            assertEquals(200.0, res.getValid().get(0).getRemanent());
        }

        @Test
        @DisplayName("Q period: first-in-list wins when same start date")
        void qPeriodFirstInListWinsOnTie() {
            TemporalFilterRequest req = TemporalFilterRequest.builder()
                    .q(List.of(
                            new QPeriod(100, "2023-07-01 00:00:00", "2023-07-31 23:59:59"),
                            new QPeriod(200, "2023-07-01 00:00:00", "2023-08-31 23:59:59")
                    ))
                    .p(List.of())
                    .k(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")))
                    .wage(50000)
                    .transactions(List.of(new Expense("2023-07-15 12:00:00", 250)))
                    .build();

            TemporalFilterResponse res = service.filterTransactions(req);
            // Same start date → first in list (fixed=100) wins
            assertEquals(100.0, res.getValid().get(0).getRemanent());
        }

        @Test
        @DisplayName("P periods stack: multiple extras are summed")
        void pPeriodsStack() {
            TemporalFilterRequest req = TemporalFilterRequest.builder()
                    .q(List.of())
                    .p(List.of(
                            new PPeriod(10, "2023-10-01 00:00:00", "2023-12-31 23:59:59"),
                            new PPeriod(20, "2023-10-01 00:00:00", "2023-11-30 23:59:59")
                    ))
                    .k(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")))
                    .wage(50000)
                    .transactions(List.of(new Expense("2023-10-15 12:00:00", 250)))
                    .build();

            TemporalFilterResponse res = service.filterTransactions(req);
            // remanent = 50 + 10 + 20 = 80
            assertEquals(80.0, res.getValid().get(0).getRemanent());
        }

        @Test
        @DisplayName("Q then P: q replaces first, then p adds on top")
        void qThenPInteraction() {
            TemporalFilterRequest req = TemporalFilterRequest.builder()
                    .q(List.of(new QPeriod(0, "2023-10-01 00:00:00", "2023-10-31 23:59:59")))
                    .p(List.of(new PPeriod(25, "2023-10-01 00:00:00", "2023-12-31 23:59:59")))
                    .k(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")))
                    .wage(50000)
                    .transactions(List.of(new Expense("2023-10-15 12:00:00", 250)))
                    .build();

            TemporalFilterResponse res = service.filterTransactions(req);
            // q sets remanent to 0, then p adds 25 → final = 25
            assertEquals(25.0, res.getValid().get(0).getRemanent());
        }
    }

    // ======================== Endpoint 4: Returns ========================

    @Nested
    @DisplayName("Returns calculation — NPS and Index")
    class ReturnsTests {

        private ReturnCalculationRequest buildProblemStatementRequest() {
            return ReturnCalculationRequest.builder()
                    .age(29)
                    .wage(50000)
                    .inflation(5.5)
                    .q(List.of(new QPeriod(0, "2023-07-01 00:00:00", "2023-07-31 23:59:59")))
                    .p(List.of(new PPeriod(25, "2023-10-01 08:00:00", "2023-12-31 19:59:59")))
                    .k(List.of(
                            new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59"),
                            new KPeriod("2023-03-01 00:00:00", "2023-11-30 23:59:59")
                    ))
                    .transactions(List.of(
                            new Expense("2023-02-28 15:49:20", 375),
                            new Expense("2023-07-01 21:59:00", 620),
                            new Expense("2023-10-12 20:15:30", 250),
                            new Expense("2023-12-17 08:09:45", 480),
                            new Expense("2023-12-17 08:09:45", -10)
                    ))
                    .build();
        }

        @Test
        @DisplayName("NPS: totalTransactionAmount = 1725 (valid transactions only)")
        void npsTotalAmount() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            assertEquals(1725.0, res.getTotalTransactionAmount());
        }

        @Test
        @DisplayName("NPS: totalCeiling = 1900")
        void npsTotalCeiling() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            assertEquals(1900.0, res.getTotalCeiling());
        }

        @Test
        @DisplayName("NPS: 2 k periods in output (same size as k input)")
        void npsKPeriodCount() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            assertEquals(2, res.getSavingsByDates().size());
        }

        @Test
        @DisplayName("NPS k1 (Jan-Dec): amount=145, profit≈86.88, taxBenefit=0")
        void npsFirstKPeriod() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            NpsReturnResponse.NpsSavingsByDate k1 = res.getSavingsByDates().get(0);

            assertEquals(145.0, k1.getAmount());
            assertEquals(86.88, k1.getProfit(), 0.5);
            assertEquals(0.0, k1.getTaxBenefit());
        }

        @Test
        @DisplayName("NPS k2 (Mar-Nov): amount=75, profit≈44.94, taxBenefit=0")
        void npsSecondKPeriod() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            NpsReturnResponse.NpsSavingsByDate k2 = res.getSavingsByDates().get(1);

            assertEquals(75.0, k2.getAmount());
            assertEquals(44.94, k2.getProfit(), 0.5);
            assertEquals(0.0, k2.getTaxBenefit());
        }

        @Test
        @DisplayName("Index k1 (Jan-Dec): amount=145, return≈1829.5")
        void indexFirstKPeriod() {
            IndexReturnResponse res = service.calculateIndexReturns(buildProblemStatementRequest());
            IndexReturnResponse.IndexSavingsByDate k1 = res.getSavingsByDates().get(0);

            assertEquals(145.0, k1.getAmount());
            assertEquals(1829.5, k1.getReturnValue(), 5.0);
            assertEquals(0.0, k1.getTaxBenefit());
        }

        @Test
        @DisplayName("Index: totalTransactionAmount and totalCeiling match NPS")
        void indexTotalsMatchNps() {
            ReturnCalculationRequest req = buildProblemStatementRequest();
            NpsReturnResponse nps = service.calculateNpsReturns(req);
            IndexReturnResponse idx = service.calculateIndexReturns(req);

            assertEquals(nps.getTotalTransactionAmount(), idx.getTotalTransactionAmount());
            assertEquals(nps.getTotalCeiling(), idx.getTotalCeiling());
        }

        @Test
        @DisplayName("Negative transaction (-10) is excluded from totals")
        void negativeExcludedFromTotals() {
            NpsReturnResponse res = service.calculateNpsReturns(buildProblemStatementRequest());
            // 375 + 620 + 250 + 480 = 1725 (not 1715 which would include -10)
            assertEquals(1725.0, res.getTotalTransactionAmount());
        }
    }
}
