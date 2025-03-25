# Stage 1: Build the application
FROM eclipse-temurin:11-jdk-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw* pom.xml ./

# Copy the Maven wrapper configuration
COPY .mvn .mvn

# Copy the source code
COPY src ./src

# Grant execution permission to Maven wrapper
RUN chmod +x mvnw

# Build the application and skip tests
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:11-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/bankid-registrator-0.0.1-SNAPSHOT.jar /app/app.jar

# Set environment variables
ENV LOG_PATH=/app/logs
ENV STORAGE_PATH=/app/storage
ENV TZ=Europe/Prague

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/app.jar"]