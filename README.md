# InvestSmart — Automated Retirement Micro-Savings Engine

> Rounds up daily expenses to the nearest ₹100 and invests the spare change in NPS or NIFTY 50 Index Funds, with temporal constraints and inflation-adjusted projections.

---

## Quick Start

### Prerequisites
- Java 21 (LTS)
- Maven 3.9+
- Docker (optional)

### Run Locally
```bash
mvn clean package -DskipTests
java -jar target/invest-smart.jar
```
Application starts on **http://localhost:5477**

### Run with Docker
```bash
docker compose up --build
```

### Run Tests
```bash
mvn clean test
```

### Swagger UI
```
http://localhost:5477/swagger-ui.html
```

---

## API Endpoints

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/blackrock/challenge/v1/transactions:parse` | Parse expenses → ceiling + remanent |
| 2 | POST | `/blackrock/challenge/v1/transactions:validator` | Validate: detect negatives + duplicates |
| 3 | POST | `/blackrock/challenge/v1/transactions:filter` | Full pipeline with temporal constraints (q/p/k) |
| 4a | POST | `/blackrock/challenge/v1/returns:nps` | NPS returns: profit + tax benefit per k period |
| 4b | POST | `/blackrock/challenge/v1/returns:index` | NIFTY 50 returns per k period |
| 5 | GET | `/blackrock/challenge/v1/performance` | System metrics: uptime, memory, threads |
| 6 | POST | `/blackrock/challenge/v1/optimize` | **Innovation:** Savings optimization recommendations |
| 7 | GET | `/blackrock/challenge/v1/analytics` | **Innovation:** Request analytics (AOP-powered) |

---

## Architecture

### Technology Stack

| Component | Version | Rationale |
|-----------|---------|-----------|
| Java | 21 (LTS) | Virtual threads, records, pattern matching, sequenced collections |
| Spring Boot | 3.5.11 | Latest stable 3.x, full virtual threads support, mature ecosystem |
| Spring Security | 6.x | Security headers (X-Frame-Options, Content-Type-Options, Cache-Control) |
| Spring AOP | 6.x | Request tracking aspect for observability |
| Springdoc OpenAPI | 2.8.15 | Interactive Swagger UI documentation |
| JaCoCo | 0.8.11 | Code coverage reporting |
| Docker | Alpine Linux | Multi-stage build, ~200MB final image, non-root user |

### Project Structure

```
com.blackrock.investsmart/
├── config/                 # SecurityConfig, WebConfig (CORS)
├── controller/             # 3 controllers split by domain
│   ├── TransactionController   (endpoints 1, 2, 3)
│   ├── ReturnsController       (endpoints 4a, 4b, optimizer)
│   └── SystemController        (endpoint 5, analytics)
├── service/                # Business logic layer
│   ├── TransactionProcessingService  (core 5-step pipeline)
│   ├── SavingsOptimizationService    (innovation: optimizer)
│   └── RequestAnalyticsService       (innovation: observability)
├── model/
│   ├── domain/             # Expense, Transaction, QPeriod, PPeriod, KPeriod
│   ├── request/            # 3 request DTOs
│   └── response/           # 8 response DTOs
├── util/                   # Pure stateless utilities
│   ├── FinancialCalculator         (math: ceiling, tax, compound interest)
│   └── DateTimeParser              (timestamp parsing, range checking)
├── aspect/                 # AOP
│   └── RequestTrackingAspect       (per-endpoint timing + counting)
└── exception/              # Centralized error handling
    └── GlobalExceptionHandler      (400/500 with clean JSON)
```

### Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Pipeline | TransactionProcessingService | 5-step sequential processing: parse → validate → q → p → k |
| Stateless Service | FinancialCalculator | Pure math functions, trivially testable and parallelizable |
| Fail-Fast Validation | validateTransactions() | Rejects negatives/duplicates before expensive q/p processing |
| DTO Separation | model/domain, request, response | Clear input/output/domain boundaries |
| Thin Controller | All controllers | 2-3 lines per method, zero business logic |
| AOP Observability | RequestTrackingAspect | Cross-cutting concern without modifying controller code |
| Global Error Handler | GlobalExceptionHandler | Consistent error responses, no stack trace leaks |

### Processing Pipeline

```
Raw Expenses
    │
    ▼
[1. Parse] ──── ceiling = ceil(amount/100) × 100
    │            remanent = ceiling - amount
    ▼
[2. Validate] ── Negative? → reject ("Negative amounts are not allowed")
    │             Duplicate (date+amount)? → reject ("Duplicate transaction")
    ▼
[3. Apply Q] ── Multiple match? → latest start date wins → first-in-list breaks ties
    │            REPLACES remanent with fixed amount
    ▼
[4. Apply P] ── All matching P periods STACK (sum extras)
    │            ADDS extra to remanent (applied AFTER Q)
    ▼
[5. Group K] ── Sum adjusted remanents per k period
    │            Transaction can belong to multiple k periods
    ▼
[6. Returns] ── NPS: A = P×(1.0711)^t → A_real = A/(1+inf)^t → profit = A_real - P
                Index: A = P×(1.1449)^t → A_real = A/(1+inf)^t → return = A_real
                Tax: benefit = Tax(income) - Tax(income - deduction)
```

---

## Trade-offs & Design Decisions

### 1. `double` over `BigDecimal`
**Choice:** All financial calculations use `double`.
**Trade-off:** Performance vs absolute precision.
**Justification:** Amounts capped at 5×10⁵, 2 decimal places — `double` provides 15-16 significant digits, more than sufficient. `BigDecimal` is 10-50x slower, and with up to 10⁶ transactions × 10⁶ periods, performance matters. Problem statement outputs (86.88, 44.94, 1829.5) are well within `double` precision range.

### 2. Single Service vs Microservices
**Choice:** One `TransactionProcessingService` owns the entire pipeline.
**Trade-off:** Simplicity vs separation.
**Justification:** Single-domain problem — splitting into ParseService, ValidateService, QPService etc. would create 5 classes with 1 method each. Method-level separation within one service is sufficient and more readable.

### 3. In-Memory vs Database
**Choice:** No database, no Redis, no external state.
**Trade-off:** Persistence vs latency.
**Justification:** Every request is self-contained — all data arrives in the request body, nothing to look up from previous requests. Adding a database would add latency for zero benefit.

### 4. Alpine Linux Docker Image
**Choice:** `eclipse-temurin:21-jre-alpine` (~200MB final image).
**Trade-off:** Smaller image vs broader compatibility.
**Justification:** Reduced attack surface (fewer packages = fewer CVEs), faster container startup, lower storage/transfer costs.

### 5. Virtual Threads
**Choice:** `spring.threads.virtual.enabled=true`.
**Trade-off:** New feature vs proven thread pool.
**Justification:** Java 21 virtual threads handle 10,000+ concurrent requests without thread pool exhaustion. One line of config, zero code changes, massive scalability improvement.

### 6. Spring Security (without Auth)
**Choice:** Include Security starter but permit all endpoints.
**Trade-off:** Dependency overhead vs security headers.
**Justification:** Free production hardening — every response includes X-Frame-Options, X-Content-Type-Options, Cache-Control headers. Shows security awareness without blocking evaluation.

---

## Innovation Features

### Savings Optimizer (`POST /optimize`)
Goes beyond calculating returns to provide **actionable recommendations**:
- NPS vs Index Fund side-by-side comparison per k period
- Q period impact analysis — shows how much savings was lost to fixed overrides
- P period boost tracking — quantifies the benefit of proactive extra savings
- Tax bracket-aware recommendations — guides instrument selection based on income

### Request Analytics (`GET /analytics`)
AOP-powered observability — zero modifications to business logic:
- Per-endpoint call counts and average processing times
- Thread-safe `ConcurrentHashMap` + `AtomicLong` counters
- Demonstrates Spring AOP pattern and production monitoring mindset

---

## Scaling Considerations

The current design is production-ready for the hackathon constraints. For a production deployment at scale:

| Concern | Current | Production Enhancement |
|---------|---------|----------------------|
| Concurrency | Virtual threads | Already handles 10K+ concurrent requests |
| State | Stateless | Horizontally scalable — run N instances behind a load balancer |
| Period matching | O(n × m) linear scan | Interval tree for O(log n + m) per query |
| Rate limiting | Not implemented | Redis-backed sliding window via Spring Cloud Gateway |
| Caching | Not needed | Request hash-keyed cache if duplicate payloads occur |
| Monitoring | In-memory analytics | Micrometer → Prometheus → Grafana pipeline |
| Auth | Permit all | JWT/OAuth2 with role-based access control |

---

## Testing

| Layer | File | Coverage |
|-------|------|----------|
| Unit | `FinancialCalculatorTest` | All math functions, parameterized, edge cases |
| Unit | `DateTimeParserTest` | Parsing, formatting, inclusive range checking |
| Unit | `TransactionProcessingServiceTest` | q/p/k rules, validation, full pipeline |
| Integration | `TransactionControllerTest` | HTTP endpoints 1-3, exact JSON verification |
| Integration | `ReturnsControllerTest` | HTTP endpoints 4a, 4b, 5, problem statement examples |
| Smoke | `InvestSmartApplicationTest` | Spring context loads successfully |

Run with coverage: `mvn clean test` → Reports at `target/site/jacoco/index.html`

---

## Author
Built for BlackRock Hacking India '26 — Financial Well-being Challenge
