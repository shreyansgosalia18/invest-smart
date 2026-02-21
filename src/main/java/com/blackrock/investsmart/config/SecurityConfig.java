package com.blackrock.investsmart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for InvestSmart.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>CSRF disabled — stateless REST API, no browser sessions or cookies</li>
 *   <li>All endpoints permitted — evaluator/judges need unrestricted access</li>
 *   <li>Security headers kept ON — free production hardening (X-Content-Type-Options,
 *       X-Frame-Options, Cache-Control, X-XSS-Protection)</li>
 *   <li>Stateless session — no server-side session storage, horizontally scalable</li>
 * </ul>
 *
 * <p>In a production deployment, this would be extended with:
 * <ul>
 *   <li>JWT/OAuth2 authentication for API consumers</li>
 *   <li>Rate limiting via a gateway or filter</li>
 *   <li>IP whitelisting for internal endpoints like /performance and /analytics</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — stateless REST API (no cookies/sessions)
                .csrf(csrf -> csrf.disable())

                // Permit all endpoints — required for hackathon evaluation
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // Stateless session — no server-side session, enables horizontal scaling
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Security headers are enabled by default:
                // - X-Content-Type-Options: nosniff
                // - X-Frame-Options: DENY
                // - Cache-Control: no-cache, no-store
                // - X-XSS-Protection: 0 (modern browsers use CSP instead)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                );

        return http.build();
    }
}
