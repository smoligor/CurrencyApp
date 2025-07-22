# Use official OpenJDK 17 as base image
FROM openjdk:17-jdk-slim


# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (for better caching)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Start application directly without external script
ENTRYPOINT ["java","-jar","/app/target/CurrencyApp-0.0.1-SNAPSHOT.jar"]
