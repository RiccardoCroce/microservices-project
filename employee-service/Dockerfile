# ── STAGE 1: Build ──────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -q

# ── STAGE 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# Crea utente non-root "spring" (best practice sicurezza)
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Assegna i file all'utente spring
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
