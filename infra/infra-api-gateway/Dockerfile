FROM eclipse-temurin:21-jre-alpine

ADD target/api-gateway-service.jar app.jar

EXPOSE 9094

ENTRYPOINT ["java", "-jar", "/app.jar"]
