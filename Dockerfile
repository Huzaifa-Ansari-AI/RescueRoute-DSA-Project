# ==============================================================================
# RescueRoute Backend — Production Multi-Stage Dockerfile (Root repository)
# ==============================================================================

# --- Stage 1: Build JAR ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM from backend
COPY backend/mvnw .
COPY backend/.mvn .mvn
COPY backend/pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy backend source code and build
COPY backend/src ./src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Minimal Production JRE Image ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=build /app/target/rescueroute-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
