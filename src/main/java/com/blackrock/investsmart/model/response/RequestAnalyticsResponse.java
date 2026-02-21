package com.blackrock.investsmart.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response for the Analytics endpoint — per-endpoint request tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAnalyticsResponse {

    /** Total requests across all endpoints */
    private long totalRequests;

    /** Per-endpoint statistics */
    private Map<String, EndpointStats> endpoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointStats {
        private long totalCalls;
        private long totalProcessingTimeMs;
        private double averageProcessingTimeMs;
    }
}
