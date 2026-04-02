FROM gcr.io/distroless/java21-debian13

ADD target/appointment-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app.jar"]