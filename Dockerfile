FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -q -e -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -q -DskipTests clean package



FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080