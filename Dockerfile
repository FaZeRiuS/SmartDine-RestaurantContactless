FROM maven:3.9.9-eclipse-temurin-25 AS build

WORKDIR /app

# 1. Copy only the files needed for dependency resolution
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Ensure mvnw has executable permissions
RUN chmod +x mvnw

# 2. Download dependencies and cache them in a Docker layer.
# We add resilience flags:
# -B: batch mode
# -Dmaven.wagon.http.retryHandler.count=5: increase retries for unstable connections
# -Dhttp.keepAlive=false: can prevent 'Premature end' errors on some networks
RUN ./mvnw dependency:go-offline -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dhttp.keepAlive=false

# 3. Copy source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Final stage: Run the application
FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
