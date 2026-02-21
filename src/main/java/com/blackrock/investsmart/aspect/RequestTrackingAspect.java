package com.blackrock.investsmart.aspect;

import com.blackrock.investsmart.service.RequestAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that intercepts all controller methods to track:
 * - Request count per endpoint
 * - Processing time per request
 *
 * <p>Uses {@code @Around} advice to measure elapsed time without
 * modifying any controller code. This is the observability layer.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestTrackingAspect {

    private final RequestAnalyticsService analyticsService;

    @Around("execution(* com.blackrock.investsmart.controller..*(..))")
    public Object trackRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String endpoint = resolveEndpoint(joinPoint);
            analyticsService.recordRequest(endpoint, duration);
            log.debug("Endpoint {} processed in {}ms", endpoint, duration);
        }
    }

    private String resolveEndpoint(ProceedingJoinPoint joinPoint) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getMethod() + " " + request.getRequestURI();
            }
        } catch (Exception e) {
            // Fallback to method name if request context unavailable
        }
        return joinPoint.getSignature().toShortString();
    }
}
