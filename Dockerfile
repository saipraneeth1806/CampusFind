# ==========================================
# CampusFind - Cloud Production Multi-Stage Dockerfile
# Optimized for Render, Railway, Fly.io, AWS, and Docker
# ==========================================

# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile & package
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring && \
    mkdir -p /app/uploads && chown -R spring:spring /app

USER spring:spring

# Copy built JAR from builder
COPY --from=builder /workspace/target/*.jar app.jar

# Cloud platform environment defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check for cloud orchestrators (Render, Railway, Kubernetes)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
