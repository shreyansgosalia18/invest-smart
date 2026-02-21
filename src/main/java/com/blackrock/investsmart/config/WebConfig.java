package com.blackrock.investsmart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration — allows cross-origin requests for API evaluation.
 *
 * <p>In production, this would be restricted to specific allowed origins.
 * For the hackathon, we allow all origins so evaluators/judges can test from
 * any tool (Postman, curl, browser, HackerRank platform).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
