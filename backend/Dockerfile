# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Build context is the repository root: the frozen contract (api/) is packaged with the app
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline -B
COPY api api
COPY backend/src backend/src
RUN mvn -f backend/pom.xml package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
