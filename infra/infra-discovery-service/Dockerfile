FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

ADD target/discovery-service.jar discovery-service.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/discovery-service.jar"]