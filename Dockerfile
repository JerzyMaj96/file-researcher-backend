#-----BUILD stage------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Gives execute permissions to mvnw
RUN chmod +x mvnw
# Download all dependencies offline to leverage Docker cache
RUN ./mvnw -q -e -DskipTests dependency:go-offline

# Copy source code and build the JAR
COPY src ./src
RUN ./mvnw -q -DskipTests clean package

#-----RYNTIME stage------
FROM eclipse-temurin:21-jdk

# Create a non-root user for running the application
RUN useradd -ms /bin/bash spring
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Environment variable for JVM options
ENV JAVA_OPTS=""

EXPOSE 8080
USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
