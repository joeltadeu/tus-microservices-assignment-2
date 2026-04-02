FROM gcr.io/distroless/java21-debian13

ADD target/auth-service.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app.jar"]