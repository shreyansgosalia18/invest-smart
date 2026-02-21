package com.blackrock.investsmart.util;

// Test type: Unit
// Validation: Verifies timestamp parsing, formatting, and inclusive range checking
// Command: mvn test -Dtest=DateTimeParserTest

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeParserTest {

    // ======================== Parsing ========================

    @Nested
    @DisplayName("parse — timestamp string to LocalDateTime")
    class ParseTests {

        @Test
        @DisplayName("Parses valid timestamp from problem statement")
        void parsesValidTimestamp() {
            LocalDateTime result = DateTimeParser.parse("2023-10-12 20:15:30");
            assertNotNull(result);
            assertEquals(2023, result.getYear());
            assertEquals(10, result.getMonthValue());
            assertEquals(12, result.getDayOfMonth());
            assertEquals(20, result.getHour());
            assertEquals(15, result.getMinute());
            assertEquals(30, result.getSecond());
        }

        @Test
        @DisplayName("Parses midnight timestamp")
        void parsesMidnight() {
            LocalDateTime result = DateTimeParser.parse("2023-01-01 00:00:00");
            assertNotNull(result);
            assertEquals(0, result.getHour());
            assertEquals(0, result.getMinute());
            assertEquals(0, result.getSecond());
        }

        @Test
        @DisplayName("Parses end-of-day timestamp")
        void parsesEndOfDay() {
            LocalDateTime result = DateTimeParser.parse("2023-12-31 23:59:59");
            assertNotNull(result);
            assertEquals(23, result.getHour());
            assertEquals(59, result.getMinute());
            assertEquals(59, result.getSecond());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "not-a-date", "2023-13-01 00:00:00", "2023/10/12 20:15:30"})
        @DisplayName("Returns null for invalid inputs")
        void returnsNullForInvalidInputs(String input) {
            assertNull(DateTimeParser.parse(input));
        }

        @Test
        @DisplayName("Handles leading/trailing whitespace")
        void handlesWhitespace() {
            LocalDateTime result = DateTimeParser.parse("  2023-10-12 20:15:30  ");
            assertNotNull(result);
            assertEquals(12, result.getDayOfMonth());
        }
    }

    // ======================== Formatting ========================

    @Nested
    @DisplayName("format — LocalDateTime to timestamp string")
    class FormatTests {

        @Test
        @DisplayName("Formats correctly")
        void formatsCorrectly() {
            LocalDateTime dt = LocalDateTime.of(2023, 10, 12, 20, 15, 30);
            assertEquals("2023-10-12 20:15:30", DateTimeParser.format(dt));
        }

        @Test
        @DisplayName("Null returns null")
        void nullReturnsNull() {
            assertNull(DateTimeParser.format(null));
        }

        @Test
        @DisplayName("Round-trip: parse → format → parse gives same result")
        void roundTrip() {
            String original = "2023-07-01 21:59:00";
            LocalDateTime parsed = DateTimeParser.parse(original);
            String formatted = DateTimeParser.format(parsed);
            assertEquals(original, formatted);
        }
    }

    // ======================== Range Checking ========================

    @Nested
    @DisplayName("isWithinRange — inclusive boundary checking")
    class RangeTests {

        private final String rangeStart = "2023-07-01 00:00:00";
        private final String rangeEnd = "2023-07-31 23:59:59";

        @Test
        @DisplayName("Date inside range returns true")
        void insideRange() {
            LocalDateTime mid = DateTimeParser.parse("2023-07-15 12:00:00");
            assertTrue(DateTimeParser.isWithinRange(mid, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Date exactly at start boundary returns true (inclusive)")
        void atStartBoundary() {
            LocalDateTime start = DateTimeParser.parse("2023-07-01 00:00:00");
            assertTrue(DateTimeParser.isWithinRange(start, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Date exactly at end boundary returns true (inclusive)")
        void atEndBoundary() {
            LocalDateTime end = DateTimeParser.parse("2023-07-31 23:59:59");
            assertTrue(DateTimeParser.isWithinRange(end, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Date before range returns false")
        void beforeRange() {
            LocalDateTime before = DateTimeParser.parse("2023-06-30 23:59:59");
            assertFalse(DateTimeParser.isWithinRange(before, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Date after range returns false")
        void afterRange() {
            LocalDateTime after = DateTimeParser.parse("2023-08-01 00:00:00");
            assertFalse(DateTimeParser.isWithinRange(after, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Null timestamp returns false")
        void nullTimestamp() {
            assertFalse(DateTimeParser.isWithinRange(null, rangeStart, rangeEnd));
        }

        @Test
        @DisplayName("Null start returns false")
        void nullStart() {
            LocalDateTime ts = DateTimeParser.parse("2023-07-15 12:00:00");
            assertFalse(DateTimeParser.isWithinRange(ts, (LocalDateTime) null, DateTimeParser.parse(rangeEnd)));
        }

        @Test
        @DisplayName("Problem statement: Oct 12 in Oct-Dec p period → true")
        void problemStatementPPeriodCheck() {
            LocalDateTime oct12 = DateTimeParser.parse("2023-10-12 20:15:30");
            assertTrue(DateTimeParser.isWithinRange(oct12, "2023-10-01 08:00:00", "2023-12-31 19:59:59"));
        }

        @Test
        @DisplayName("Problem statement: Jul 1 in Jul q period → true")
        void problemStatementQPeriodCheck() {
            LocalDateTime jul1 = DateTimeParser.parse("2023-07-01 21:59:00");
            assertTrue(DateTimeParser.isWithinRange(jul1, "2023-07-01 00:00:00", "2023-07-31 23:59:59"));
        }

        @Test
        @DisplayName("Problem statement: Feb 28 NOT in Mar-Nov k period → false")
        void problemStatementKPeriodExclusion() {
            LocalDateTime feb28 = DateTimeParser.parse("2023-02-28 15:49:20");
            assertFalse(DateTimeParser.isWithinRange(feb28, "2023-03-01 00:00:00", "2023-11-30 23:59:59"));
        }
    }
}
