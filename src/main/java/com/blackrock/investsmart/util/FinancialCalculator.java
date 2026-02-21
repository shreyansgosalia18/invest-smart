package com.blackrock.investsmart.util;

/**
 * Pure financial calculation utilities.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>All methods are static and stateless — trivially testable and parallelizable</li>
 *   <li>Uses {@code double} over {@code BigDecimal} for performance.
 *       With amounts capped at 5×10⁵ and 2 decimal places, double's 15-16 significant
 *       digits provide more than sufficient precision. This is a deliberate trade-off:
 *       10-50x faster computation matters when processing up to 10⁶ transactions.</li>
 *   <li>Constants are package-visible for test assertions</li>
 * </ul>
 */
public final class FinancialCalculator {

    // ======================== Investment Rates ========================

    /** NPS annual interest rate (7.11%) */
    static final double NPS_ANNUAL_RATE = 0.0711;

    /** NIFTY 50 Index Fund annual interest rate (14.49%) */
    static final double INDEX_ANNUAL_RATE = 0.1449;

    /** Retirement age — investment horizon is max(RETIREMENT_AGE - currentAge, MIN_INVESTMENT_YEARS) */
    static final int RETIREMENT_AGE = 60;

    /** Minimum investment period regardless of age */
    static final int MIN_INVESTMENT_YEARS = 5;

    /** Maximum NPS deduction for tax benefit */
    static final double MAX_NPS_DEDUCTION = 200_000.0;

    /** NPS deduction percentage of annual income */
    static final double NPS_DEDUCTION_PERCENTAGE = 0.10;

    // ======================== Tax Slab Boundaries ========================

    private static final double SLAB_1_LIMIT = 700_000.0;   // 0%
    private static final double SLAB_2_LIMIT = 1_000_000.0;  // 10%
    private static final double SLAB_3_LIMIT = 1_200_000.0;  // 15%
    private static final double SLAB_4_LIMIT = 1_500_000.0;  // 20%
    // Above SLAB_4_LIMIT: 30%

    private FinancialCalculator() {
        // Utility class — prevent instantiation
    }

    // ======================== Ceiling & Remanent ========================

    /**
     * Rounds an amount up to the next multiple of 100.
     *
     * <p>Examples:
     * <ul>
     *   <li>250 → 300</li>
     *   <li>1519 → 1600</li>
     *   <li>100 → 100 (already a multiple)</li>
     *   <li>0 → 0</li>
     * </ul>
     *
     * @param amount the expense amount (must be non-negative)
     * @return the next multiple of 100, or the amount itself if already a multiple
     */
    public static double ceiling100(double amount) {
        if (amount <= 0) return 0;
        return Math.ceil(amount / 100.0) * 100.0;
    }

    /**
     * Calculates the remanent (spare change) for a given expense.
     * This is the amount that gets auto-saved for investment.
     *
     * @param amount the expense amount
     * @return ceiling - amount
     */
    public static double remanent(double amount) {
        return ceiling100(amount) - amount;
    }

    // ======================== Investment Period ========================

    /**
     * Calculates the number of years until retirement.
     *
     * <p>From the problem: "t is the difference between 60 years and your age,
     * assuming it is less than 60, otherwise 5."
     *
     * @param age current age of the investor
     * @return max(60 - age, 5)
     */
    public static int investmentYears(int age) {
        return Math.max(RETIREMENT_AGE - age, MIN_INVESTMENT_YEARS);
    }

    // ======================== Compound Interest ========================

    /**
     * Calculates compound interest with annual compounding.
     * Formula: A = P × (1 + r)^t
     *
     * @param principal initial investment amount (P)
     * @param annualRate annual interest rate as decimal (e.g., 0.0711 for 7.11%)
     * @param years number of compounding years (t)
     * @return final amount (A)
     */
    public static double compoundInterest(double principal, double annualRate, int years) {
        if (principal <= 0 || years <= 0) return principal;
        return principal * Math.pow(1 + annualRate, years);
    }

    // ======================== Inflation Adjustment ========================

    /**
     * Adjusts a nominal amount for inflation to get the real value.
     * Formula: A_real = A / (1 + inflation)^t
     *
     * @param nominalAmount the pre-inflation amount
     * @param inflationRate annual inflation rate as decimal (e.g., 0.055 for 5.5%)
     * @param years number of years
     * @return inflation-adjusted (real) amount
     */
    public static double adjustForInflation(double nominalAmount, double inflationRate, int years) {
        if (years <= 0) return nominalAmount;
        return nominalAmount / Math.pow(1 + inflationRate, years);
    }

    // ======================== Tax Calculation ========================

    /**
     * Calculates income tax using simplified slab rates.
     *
     * <p>Slabs:
     * <ul>
     *   <li>₹0 to ₹7,00,000: 0%</li>
     *   <li>₹7,00,001 to ₹10,00,000: 10%</li>
     *   <li>₹10,00,001 to ₹12,00,000: 15%</li>
     *   <li>₹12,00,001 to ₹15,00,000: 20%</li>
     *   <li>Above ₹15,00,000: 30%</li>
     * </ul>
     *
     * @param annualIncome gross annual income in rupees
     * @return total tax amount
     */
    public static double calculateTax(double annualIncome) {
        if (annualIncome <= SLAB_1_LIMIT) return 0;

        double tax = 0;
        double remaining = annualIncome;

        if (remaining > SLAB_4_LIMIT) {
            tax += (remaining - SLAB_4_LIMIT) * 0.30;
            remaining = SLAB_4_LIMIT;
        }
        if (remaining > SLAB_3_LIMIT) {
            tax += (remaining - SLAB_3_LIMIT) * 0.20;
            remaining = SLAB_3_LIMIT;
        }
        if (remaining > SLAB_2_LIMIT) {
            tax += (remaining - SLAB_2_LIMIT) * 0.15;
            remaining = SLAB_2_LIMIT;
        }
        if (remaining > SLAB_1_LIMIT) {
            tax += (remaining - SLAB_1_LIMIT) * 0.10;
        }

        return tax;
    }

    // ======================== NPS Tax Benefit ========================

    /**
     * Calculates the tax benefit from NPS investment.
     *
     * <p>Formula:
     * <ol>
     *   <li>NPS_Deduction = min(invested, 10% of annual_income, ₹2,00,000)</li>
     *   <li>Tax_Benefit = Tax(income) - Tax(income - NPS_Deduction)</li>
     * </ol>
     *
     * @param invested amount invested in NPS for this k period
     * @param monthlyWage monthly salary in rupees
     * @return tax benefit amount (returned separately, does not generate interest)
     */
    public static double calculateNpsTaxBenefit(double invested, double monthlyWage) {
        double annualIncome = monthlyWage * 12;
        double npsDeduction = Math.min(invested, Math.min(NPS_DEDUCTION_PERCENTAGE * annualIncome, MAX_NPS_DEDUCTION));

        double taxWithout = calculateTax(annualIncome);
        double taxWith = calculateTax(annualIncome - npsDeduction);

        return round2(taxWithout - taxWith);
    }

    // ======================== Full Return Calculations ========================

    /**
     * Calculates NPS investment profit (inflation-adjusted).
     *
     * <p>Pipeline: invested → compound at 7.11% → adjust for inflation → subtract principal = profit
     *
     * @param invested principal amount
     * @param years investment period
     * @param inflationPercent inflation rate as percentage (e.g., 5.5 for 5.5%)
     * @return real profit (A_real - invested)
     */
    public static double npsProfit(double invested, int years, double inflationPercent) {
        double compounded = compoundInterest(invested, NPS_ANNUAL_RATE, years);
        double realValue = adjustForInflation(compounded, inflationPercent / 100.0, years);
        return round2(realValue - invested);
    }

    /**
     * Calculates NIFTY 50 Index Fund return (inflation-adjusted).
     *
     * <p>Pipeline: invested → compound at 14.49% → adjust for inflation = total real value
     *
     * <p>Note: Index returns the <em>total</em> real value, not just profit.
     * This differs from NPS which returns profit (real_value - invested).
     *
     * @param invested principal amount
     * @param years investment period
     * @param inflationPercent inflation rate as percentage (e.g., 5.5 for 5.5%)
     * @return total inflation-adjusted value (A_real)
     */
    public static double indexReturn(double invested, int years, double inflationPercent) {
        double compounded = compoundInterest(invested, INDEX_ANNUAL_RATE, years);
        double realValue = adjustForInflation(compounded, inflationPercent / 100.0, years);
        return round2(realValue);
    }

    // ======================== Rounding ========================

    /**
     * Rounds a value to 2 decimal places using standard rounding.
     *
     * @param value the value to round
     * @return value rounded to 2 decimal places
     */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
