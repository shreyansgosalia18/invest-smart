package com.blackrock.investsmart.service;

import com.blackrock.investsmart.model.response.RequestAnalyticsResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory request analytics — tracks call counts and processing times per endpoint.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>ConcurrentHashMap + AtomicLong for thread-safe counters without locking</li>
 *   <li>No external dependencies (Redis etc.) — stateless and lightweight</li>
 *   <li>In production, this data would feed into Prometheus/Grafana via Micrometer</li>
 * </ul>
 */
@Service
public class RequestAnalyticsService {

    private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> totalProcessingTimeMs = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);

    /**
     * Records a completed request.
     *
     * @param endpoint the endpoint path (e.g., "/transactions:parse")
     * @param durationMs processing time in milliseconds
     */
    public void recordRequest(String endpoint, long durationMs) {
        requestCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        totalProcessingTimeMs.computeIfAbsent(endpoint, k -> new AtomicLong(0)).addAndGet(durationMs);
        totalRequests.incrementAndGet();
    }

    /**
     * Builds the analytics response with per-endpoint breakdown.
     */
    public RequestAnalyticsResponse getAnalytics() {
        Map<String, RequestAnalyticsResponse.EndpointStats> endpointStats = new java.util.LinkedHashMap<>();

        requestCounts.forEach((endpoint, count) -> {
            long calls = count.get();
            long totalMs = totalProcessingTimeMs.getOrDefault(endpoint, new AtomicLong(0)).get();
            double avgMs = calls > 0 ? (double) totalMs / calls : 0;

            endpointStats.put(endpoint, RequestAnalyticsResponse.EndpointStats.builder()
                    .totalCalls(calls)
                    .totalProcessingTimeMs(totalMs)
                    .averageProcessingTimeMs(Math.round(avgMs * 100.0) / 100.0)
                    .build());
        });

        return RequestAnalyticsResponse.builder()
                .totalRequests(totalRequests.get())
                .endpoints(endpointStats)
                .build();
    }
}
