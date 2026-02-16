# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :feed-hub-api:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/feed-hub-api/build/libs/*.jar app.jar
ENV SERVER_PORT=12001
EXPOSE 12001
ENTRYPOINT ["java", "-jar", "app.jar"]
