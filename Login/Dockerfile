FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /workspace

COPY pom.xml ./
COPY CommonLib/pom.xml CommonLib/pom.xml
COPY CommonLib/src CommonLib/src
COPY Login/pom.xml Login/pom.xml
COPY Login/src Login/src

RUN mvn -pl Login -am clean package -DskipTests

FROM eclipse-temurin:25-jre

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

COPY --from=builder /workspace/Login/target/Login-1.0-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
