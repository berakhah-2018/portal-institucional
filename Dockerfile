FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend backend
RUN mvn -B -pl backend -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home app
COPY --from=build --chown=app:app /workspace/backend/target/backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
