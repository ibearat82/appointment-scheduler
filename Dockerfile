# ---------------------------------------------------------------
# Appointment Scheduler - container image for Azure Container Apps
# Milestone 3. Multi-stage build: Maven builds the JAR, then only
# the JAR is copied into a small Java 21 runtime image.
#
# Build:  docker build --platform linux/amd64 -t ibearat82/appointment-scheduler:0.3 .
# Run:    docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... ibearat82/appointment-scheduler:0.3
# ---------------------------------------------------------------

# ---- Stage 1: build the JAR with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first so Docker can cache this layer
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Stage 2: run the JAR on a Java 21 runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Office time zone (Arizona, no daylight saving)
ENV TZ=America/Phoenix
# Let the JVM size its memory to the container limit
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
