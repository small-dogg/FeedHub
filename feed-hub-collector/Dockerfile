# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :feed-hub-collector:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Playwright system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0t64 libnss3 libnspr4 libdbus-1-3 libatk1.0-0t64 \
    libatk-bridge2.0-0t64 libcups2t64 libdrm2 libxcb1 libxkbcommon0 \
    libx11-6 libxcomposite1 libxdamage1 libxext6 libxfixes3 libxrandr2 \
    libgbm1 libpango-1.0-0 libcairo2 libasound2t64 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/feed-hub-collector/build/libs/*.jar app.jar
ENV SERVER_PORT=12004
EXPOSE 12004
ENTRYPOINT ["java", "-jar", "app.jar"]
