# ==============================================================================
# RescueRoute Backend — Production Multi-Stage Dockerfile (Java 21 / Spring Boot)
# ==============================================================================

# --- Stage 1: Build JAR ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM for dependency layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source files and package production JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Minimal Production JRE Image ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-privileged service user for production container security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy compiled JAR artifact
COPY --from=build /app/target/rescueroute-0.0.1-SNAPSHOT.jar app.jar

# Dynamic PORT provided by Render (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
