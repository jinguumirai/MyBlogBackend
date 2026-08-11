FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /workspace

COPY Gateway/pom.xml Gateway/pom.xml
COPY Gateway/src Gateway/src

RUN mvn -f Gateway/pom.xml clean package \
    org.springframework.boot:spring-boot-maven-plugin:3.4.5:repackage \
    -DskipTests

FROM eclipse-temurin:25-jre

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

COPY --from=builder /workspace/Gateway/target/Gateway-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
