# Etape 1 : Compiler le backend 
FROM  maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# .mvn/maven.config : réessayer quand Maven Central répond 429 (Too Many Requests)
COPY .mvn ./.mvn
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Les tests sont lancés dans le job CI "test", pas dans le build de l'image
RUN mvn -B -q package -DskipTests
#checkov:skip=CKV_DOCKER_2:Healthcheck défini dans docker-compose.yml.
# Etape 2 : Execution 
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]