FROM maven:3.9.9-eclipse-temurin-24 AS build

WORKDIR /app

COPY . .

RUN mvn -pl Login -am clean package -DskipTests

FROM eclipse-temurin:24-jre

ENV SPRING_PROFILES_ACTIVE=dev

WORKDIR /app

COPY --from=build /app/Login/target/Login-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]