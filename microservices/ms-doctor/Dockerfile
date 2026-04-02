FROM gcr.io/distroless/java21-debian13

ADD target/doctor-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app.jar"]