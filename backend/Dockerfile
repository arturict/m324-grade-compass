# Teil 1 (build): JAR-File mit Maven und JDK bauen
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

# Zuerst nur das pom.xml: Die Abhängigkeiten bleiben im Cache, solange es sich nicht ändert
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests


# Teil 2 (Laufzeit): nur Java (JRE) und das fertige JAR-File
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
