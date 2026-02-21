package com.blackrock.investsmart.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for the Performance Report endpoint (endpoint 5).
 *
 * <p>Reports system execution metrics:
 * <ul>
 *   <li>{@code time} — application uptime as "1970-01-01 HH:mm:ss.SSS"</li>
 *   <li>{@code memory} — heap + non-heap memory usage in MB (format: "XX.XX")</li>
 *   <li>{@code threads} — active thread count</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPerformanceResponse {

    /** Uptime formatted as "1970-01-01 HH:mm:ss.SSS" (epoch-based duration) */
    private String time;

    /** Memory usage in megabytes, format: "XX.XX" */
    private String memory;

    /** Number of active threads */
    private int threads;
}
