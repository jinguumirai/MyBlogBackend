FROM eclipse-temurin:25-jre

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

COPY Gateway/target/Gateway-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
