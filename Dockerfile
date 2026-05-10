# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

LABEL org.opencontainers.image.source="https://github.com/devhms/nightshade"
LABEL org.opencontainers.image.description="Nightshade LLM Data Poisoning Engine"
LABEL org.opencontainers.image.licenses="MIT"

COPY --from=builder /app/target/nightshade-*-all.jar /app/nightshade.jar

ENTRYPOINT ["java", "-jar", "/app/nightshade.jar"]
CMD ["--help"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD java -jar /app/nightshade.jar --version > /dev/null 2>&1 || exit 1
