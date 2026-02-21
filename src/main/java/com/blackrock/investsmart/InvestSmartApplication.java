package com.blackrock.investsmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * InvestSmart — Automated retirement micro-savings engine.
 *
 * Rounds up daily expenses to the nearest multiple of 100 and invests
 * the spare change in NPS or NIFTY 50 index funds, with support for
 * temporal constraints (q/p/k periods) and inflation-adjusted projections.
 */
@SpringBootApplication
public class InvestSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestSmartApplication.class, args);
    }
}
