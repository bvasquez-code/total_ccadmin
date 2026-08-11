# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/data/uploads /app/data/uploads
ENV APP_FILE_STORAGE_ROOT=/app/data/uploads
VOLUME ["/app/data/uploads"]
EXPOSE 9001
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
