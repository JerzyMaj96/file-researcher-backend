FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

RUN mkdir -p /app/temp-uploads && chown -R spring:spring /app/temp-uploads && chmod 755 /app/temp-uploads

COPY --from=build /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]