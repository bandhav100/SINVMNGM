# Production runtime image for Spring Boot backend
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy pre-built application jar
COPY target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
