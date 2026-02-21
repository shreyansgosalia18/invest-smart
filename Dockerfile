# docker build -t blk-hacking-ind-shreyans .

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy dependency descriptor first (Docker layer caching — dependencies rarely change)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime
# =============================================================================
# OS: Alpine Linux
# Selection criteria:
#   - Minimal footprint (~5MB base vs ~75MB Ubuntu) → final image ~200MB
#   - Reduced attack surface — fewer packages = fewer CVEs
#   - Fast container startup — critical for auto-scaling scenarios
#   - Industry standard for Java microservice containers
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Security: run as non-root user (principle of least privilege)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/invest-smart.jar app.jar

# Health check against the performance endpoint
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=30s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:5477/blackrock/challenge/v1/performance || exit 1

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 5477

ENTRYPOINT ["java", \
    "-jar", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.port=5477", \
    "app.jar"]
