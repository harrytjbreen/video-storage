FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean installDist --no-daemon

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/build/install/download-service ./download-service
CMD ["./download-service/bin/download-service"]