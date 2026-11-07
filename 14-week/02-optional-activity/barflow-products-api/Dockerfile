# ---------- Etapa 1: compilar el .jar con Maven ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom.xml: si no cambian las dependencias, Docker reutiliza esta capa
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
# Las pruebas se corren aparte con "mvn test"; aquí solo se empaqueta
RUN mvn -q package -DskipTests

# ---------- Etapa 2: imagen final, solo el JRE y el .jar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
