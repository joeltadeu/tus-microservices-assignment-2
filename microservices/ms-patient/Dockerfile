FROM gcr.io/distroless/java21-debian13

ADD target/patient-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app.jar"]
