FROM eclipse-temurin:8-jdk

RUN apt-get update && apt-get install -y maven

WORKDIR /app