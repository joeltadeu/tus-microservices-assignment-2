FROM eclipse-temurin:21-jre-alpine

ADD target/config-server.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app.jar"]
