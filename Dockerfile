FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN ./mvnw -q -e -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jdk

RUN useradd -ms /bin/bash spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
