# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package executable jar
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Production JRE runtime
FROM eclipse-temurin:17-jre-alpine

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Create upload and private document directories with non-root ownership
RUN mkdir -p /app/uploads /app/private_documents && chown -R appuser:appgroup /app

# Copy artifact from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/ngoconnect-backend-1.0.0.jar /app/app.jar

USER appuser

EXPOSE 8080

ENV PORT=8080 \
    UPLOAD_DIR=/app/uploads

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
