# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/lexiquery-1.0.0.war app.war
EXPOSE 8080
# Hosting platforms provide the port in $PORT; Spring reads SERVER_PORT
ENTRYPOINT ["sh", "-c", "SERVER_PORT=${PORT:-8080} exec java -Xmx400m -jar app.war"]
