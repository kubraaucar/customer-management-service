# Backend image: builds the Spring Boot jar, then runs it on a small Java runtime.
#   docker build -t customer-management-backend .

# ---- 1. Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Download the dependencies first. Docker reuses this step as long as pom.xml does not change,
# so rebuilding after a code change is fast.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests are not run here: they need Docker for the database and are run by CI instead.
RUN mvn -B -q package -Dmaven.test.skip=true && cp target/*.jar app.jar

# ---- 2. Run ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a normal user, not as root.
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
COPY --from=build /build/app.jar app.jar
USER spring

EXPOSE 8080
# Let Java use up to 75% of the container's memory.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
