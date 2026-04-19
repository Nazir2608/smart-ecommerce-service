# ── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress -q

COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

COPY --from=builder /build/target/smart-ecommerce-service-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.net.preferIPv4Stack=true", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]