package com.blackrock.investsmart.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date-time parsing and range-checking utilities.
 *
 * <p>All timestamps in this system use the format {@code "YYYY-MM-DD HH:mm:ss"}.
 * Period boundaries (start/end) are inclusive on both ends.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@link LocalDateTime} used (no timezone) — all dates are in the same implicit
 *       timezone as per the problem constraints</li>
 *   <li>Formatter is thread-safe and reused as a constant</li>
 *   <li>Parsing failures return {@code null} instead of throwing — caller decides
 *       how to handle invalid dates</li>
 * </ul>
 */
public final class DateTimeParser {

    /** Standard timestamp format used throughout the system */
    public static final String FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT_PATTERN);

    private DateTimeParser() {
        // Utility class — prevent instantiation
    }

    /**
     * Parses a timestamp string into a {@link LocalDateTime}.
     *
     * @param dateStr timestamp in "YYYY-MM-DD HH:mm:ss" format
     * @return parsed LocalDateTime, or {@code null} if input is null, blank, or unparseable
     */
    public static LocalDateTime parse(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(dateStr.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Formats a {@link LocalDateTime} back to the standard timestamp string.
     *
     * @param dateTime the datetime to format
     * @return formatted string in "YYYY-MM-DD HH:mm:ss" format
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(FORMATTER);
    }

    /**
     * Checks if a timestamp falls within an inclusive date range [start, end].
     *
     * <p>Both boundaries are inclusive as per the problem statement:
     * "the date ranges can intersect between different moments, and they are inclusive ranges"
     *
     * @param timestamp the datetime to check
     * @param start range start (inclusive) as string
     * @param end range end (inclusive) as string
     * @return {@code true} if timestamp is within [start, end], {@code false} otherwise
     *         (also returns false if any input is null or unparseable)
     */
    public static boolean isWithinRange(LocalDateTime timestamp, String start, String end) {
        LocalDateTime startDt = parse(start);
        LocalDateTime endDt = parse(end);
        return isWithinRange(timestamp, startDt, endDt);
    }

    /**
     * Checks if a timestamp falls within an inclusive date range [start, end].
     * Uses pre-parsed LocalDateTime values for performance in hot loops.
     *
     * @param timestamp the datetime to check
     * @param start range start (inclusive)
     * @param end range end (inclusive)
     * @return {@code true} if timestamp is within [start, end]
     */
    public static boolean isWithinRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        if (timestamp == null || start == null || end == null) return false;
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }
}
