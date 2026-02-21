package com.blackrock.investsmart.controller;

// Test type: Integration
// Validation: Full HTTP request → response for endpoints 4a, 4b, 5 with problem statement verification
// Command: mvn test -Dtest=ReturnsControllerTest

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReturnsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE = "/blackrock/challenge/v1";

    private static final String PROBLEM_STATEMENT_INPUT = """
            {
                "age": 29,
                "wage": 50000,
                "inflation": 5.5,
                "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
                "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
                "k": [
                    {"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"},
                    {"start": "2023-03-01 00:00:00", "end": "2023-11-30 23:59:59"}
                ],
                "transactions": [
                    {"date": "2023-02-28 15:49:20", "amount": 375},
                    {"date": "2023-07-01 21:59:00", "amount": 620},
                    {"date": "2023-10-12 20:15:30", "amount": 250},
                    {"date": "2023-12-17 08:09:45", "amount": 480},
                    {"date": "2023-12-17 08:09:45", "amount": -10}
                ]
            }
            """;

    // ======================== Endpoint 4a: NPS Returns ========================

    @Nested
    @DisplayName("POST /returns:nps")
    class NpsEndpointTests {

        @Test
        @DisplayName("Problem statement: totalTransactionAmount=1725, totalCeiling=1900")
        void npsTotals() throws Exception {
            mockMvc.perform(post(BASE + "/returns:nps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTransactionAmount").value(1725.0))
                    .andExpect(jsonPath("$.totalCeiling").value(1900.0));
        }

        @Test
        @DisplayName("Problem statement: 2 k periods in savingsByDates")
        void npsKPeriodCount() throws Exception {
            mockMvc.perform(post(BASE + "/returns:nps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savingsByDates.length()").value(2));
        }

        @Test
        @DisplayName("Problem statement: k1 (Jan-Dec) amount=145, profit≈86.88, taxBenefit=0")
        void npsFirstKPeriod() throws Exception {
            mockMvc.perform(post(BASE + "/returns:nps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savingsByDates[0].amount").value(145.0))
                    .andExpect(jsonPath("$.savingsByDates[0].profit").value(closeTo(86.88, 0.5)))
                    .andExpect(jsonPath("$.savingsByDates[0].taxBenefit").value(0.0));
        }

        @Test
        @DisplayName("Problem statement: k2 (Mar-Nov) amount=75, profit≈44.94, taxBenefit=0")
        void npsSecondKPeriod() throws Exception {
            mockMvc.perform(post(BASE + "/returns:nps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savingsByDates[1].amount").value(75.0))
                    .andExpect(jsonPath("$.savingsByDates[1].profit").value(closeTo(44.94, 0.5)))
                    .andExpect(jsonPath("$.savingsByDates[1].taxBenefit").value(0.0));
        }
    }

    // ======================== Endpoint 4b: Index Returns ========================

    @Nested
    @DisplayName("POST /returns:index")
    class IndexEndpointTests {

        @Test
        @DisplayName("Problem statement: k1 (Jan-Dec) amount=145, return≈1829.5")
        void indexFirstKPeriod() throws Exception {
            mockMvc.perform(post(BASE + "/returns:index")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savingsByDates[0].amount").value(145.0))
                    .andExpect(jsonPath("$.savingsByDates[0].return").value(closeTo(1829.5, 5.0)))
                    .andExpect(jsonPath("$.savingsByDates[0].taxBenefit").value(0.0));
        }

        @Test
        @DisplayName("Index totals match NPS totals")
        void indexTotalsMatch() throws Exception {
            mockMvc.perform(post(BASE + "/returns:index")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROBLEM_STATEMENT_INPUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTransactionAmount").value(1725.0))
                    .andExpect(jsonPath("$.totalCeiling").value(1900.0));
        }
    }

    // ======================== Endpoint 5: Performance ========================

    @Nested
    @DisplayName("GET /performance")
    class PerformanceEndpointTests {

        @Test
        @DisplayName("Returns time, memory, threads")
        void returnsMetrics() throws Exception {
            mockMvc.perform(get(BASE + "/performance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.time").value(startsWith("1970-01-01")))
                    .andExpect(jsonPath("$.memory").isString())
                    .andExpect(jsonPath("$.threads").isNumber());
        }
    }
}
