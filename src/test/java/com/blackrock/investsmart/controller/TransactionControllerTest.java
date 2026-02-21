package com.blackrock.investsmart.controller;

// Test type: Integration
// Validation: Full HTTP request → response for endpoints 1, 2, 3 with exact JSON from problem statement
// Command: mvn test -Dtest=TransactionControllerTest

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE = "/blackrock/challenge/v1";

    // ======================== Endpoint 1: Parse ========================

    @Nested
    @DisplayName("POST /transactions:parse")
    class ParseEndpointTests {

        @Test
        @DisplayName("Problem statement: 4 expenses → 4 transactions with ceiling/remanent")
        void parsesProblemStatementExample() throws Exception {
            String input = """
                [
                    {"date": "2023-10-12 20:15:30", "amount": 250},
                    {"date": "2023-02-28 15:49:20", "amount": 375},
                    {"date": "2023-07-01 21:59:00", "amount": 620},
                    {"date": "2023-12-17 08:09:45", "amount": 480}
                ]
                """;

            mockMvc.perform(post(BASE + "/transactions:parse")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(input))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].ceiling").value(300.0))
                    .andExpect(jsonPath("$[0].remanent").value(50.0))
                    .andExpect(jsonPath("$[1].ceiling").value(400.0))
                    .andExpect(jsonPath("$[1].remanent").value(25.0))
                    .andExpect(jsonPath("$[2].ceiling").value(700.0))
                    .andExpect(jsonPath("$[2].remanent").value(80.0))
                    .andExpect(jsonPath("$[3].ceiling").value(500.0))
                    .andExpect(jsonPath("$[3].remanent").value(20.0));
        }

        @Test
        @DisplayName("Empty list returns empty array")
        void emptyListReturnsEmpty() throws Exception {
            mockMvc.perform(post(BASE + "/transactions:parse")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Malformed JSON returns 400")
        void malformedJsonReturns400() throws Exception {
            mockMvc.perform(post(BASE + "/transactions:parse")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================== Endpoint 2: Validator ========================

    @Nested
    @DisplayName("POST /transactions:validator")
    class ValidatorEndpointTests {

        @Test
        @DisplayName("Problem statement: 3 valid, 1 negative → rejected")
        void validatorProblemStatementExample() throws Exception {
            String input = """
                {
                    "wage": 50000,
                    "transactions": [
                        {"date": "2023-01-15 10:30:00", "amount": 2000, "ceiling": 300, "remanent": 50},
                        {"date": "2023-03-20 14:45:00", "amount": 3500, "ceiling": 400, "remanent": 70},
                        {"date": "2023-06-10 09:15:00", "amount": 1500, "ceiling": 200, "remanent": 30},
                        {"date": "2023-07-10 09:15:00", "amount": -250, "ceiling": 200, "remanent": 30}
                    ]
                }
                """;

            mockMvc.perform(post(BASE + "/transactions:validator")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(input))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid.length()").value(3))
                    .andExpect(jsonPath("$.invalid.length()").value(1))
                    .andExpect(jsonPath("$.invalid[0].message").value("Negative amounts are not allowed"));
        }

        @Test
        @DisplayName("Duplicate detection: same date + amount")
        void duplicateDetection() throws Exception {
            String input = """
                {
                    "wage": 50000,
                    "transactions": [
                        {"date": "2023-10-12 20:15:30", "amount": 250, "ceiling": 300, "remanent": 50},
                        {"date": "2023-10-12 20:15:30", "amount": 250, "ceiling": 300, "remanent": 50}
                    ]
                }
                """;

            mockMvc.perform(post(BASE + "/transactions:validator")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(input))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid.length()").value(1))
                    .andExpect(jsonPath("$.invalid.length()").value(1))
                    .andExpect(jsonPath("$.invalid[0].message").value("Duplicate transaction"));
        }
    }

    // ======================== Endpoint 3: Filter ========================

    @Nested
    @DisplayName("POST /transactions:filter")
    class FilterEndpointTests {

        @Test
        @DisplayName("Problem statement filter: q=0 Jul, p=30 Oct-Dec, validates + applies periods")
        void filterProblemStatementExample() throws Exception {
            String input = """
                {
                    "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
                    "p": [{"extra": 30, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
                    "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
                    "wage": 50000,
                    "transactions": [
                        {"date": "2023-02-28 15:49:20", "amount": 375},
                        {"date": "2023-07-15 10:30:00", "amount": 620},
                        {"date": "2023-10-12 20:15:30", "amount": 250},
                        {"date": "2023-10-12 20:15:30", "amount": 250},
                        {"date": "2023-12-17 08:09:45", "amount": -480}
                    ]
                }
                """;

            mockMvc.perform(post(BASE + "/transactions:filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(input))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid.length()").value(3))
                    .andExpect(jsonPath("$.invalid.length()").value(2))
                    // Feb 28: remanent=25, no q/p
                    .andExpect(jsonPath("$.valid[0].remanent").value(25.0))
                    .andExpect(jsonPath("$.valid[0].inkPeriod").value(true))
                    // Jul 15: q sets remanent=0
                    .andExpect(jsonPath("$.valid[1].remanent").value(0.0))
                    // Oct 12: remanent=50+30=80
                    .andExpect(jsonPath("$.valid[2].remanent").value(80.0));
        }
    }
}
