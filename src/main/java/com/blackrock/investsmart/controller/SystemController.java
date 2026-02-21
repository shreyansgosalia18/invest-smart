package com.blackrock.investsmart.controller;

import com.blackrock.investsmart.model.response.RequestAnalyticsResponse;
import com.blackrock.investsmart.model.response.SystemPerformanceResponse;
import com.blackrock.investsmart.service.RequestAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * System operations — performance metrics and health monitoring.
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Tag(name = "System", description = "Performance metrics and operational monitoring")
public class SystemController {

    private final RequestAnalyticsService analyticsService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @GetMapping("/performance")
    @Operation(summary = "Report system execution metrics — uptime, memory, threads")
    public ResponseEntity<SystemPerformanceResponse> getPerformanceMetrics() {

        // Uptime: format as "1970-01-01 HH:mm:ss.SSS"
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        long millis = uptime.toMillisPart();
        String timeStr = String.format("1970-01-01 %02d:%02d:%02d.%03d", hours, minutes, seconds, millis);

        // Memory: heap + non-heap in MB
        Runtime runtime = Runtime.getRuntime();
        double usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        String memoryStr = String.format("%.2f", usedMemoryMB);

        // Threads: active count
        int threadCount = Thread.activeCount();

        return ResponseEntity.ok(SystemPerformanceResponse.builder()
                .time(timeStr)
                .memory(memoryStr)
                .threads(threadCount)
                .build());
    }

    @GetMapping("/analytics")
    @Operation(summary = "View request analytics — call counts and processing times per endpoint")
    public ResponseEntity<RequestAnalyticsResponse> getRequestAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }
}
