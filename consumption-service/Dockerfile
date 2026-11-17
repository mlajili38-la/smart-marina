# --- Étape 1 : compilation avec Maven (le JDK n'est pas conservé dans l'image finale) ---
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# Les dépendances sont mises en cache tant que pom.xml ne change pas
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# --- Étape 2 : image d'exécution légère (JRE seul) ---
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /build/target/*.jar app.jar
USER spring
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
