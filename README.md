![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Spring Cloud Config](https://img.shields.io/badge/Spring%20Cloud%20Config-2025.1.1-blue)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

# Patient Management System (PMS)

A Patient System Management platform built with Java 21, Spring Boot, and MariaDB, designed using a microservices architecture.

## 📋 Overview

The primary goal of this project is to demonstrate a complete microservices ecosystem, showcasing the infrastructure and code required to manage patient data, doctor information, appointments, and authentication. The system is deployed using [Docker Compose](https://docs.docker.com/compose/) for local development and containerised environments.

The system consists of five independent microservices:

- **[ms-auth](microservices/ms-auth)**: Handles authentication and user account management. Responsible for login, JWT token issuance, token refresh, and creating/disabling user accounts linked to domain entities.
- **[ms-doctor](microservices/ms-doctor)**: Handles the creation, retrieval, and management of doctor information.
- **[ms-patient](microservices/ms-patient)**: Manages patient data, including personal details and medical history records.
- **[ms-appointment](microservices/ms-appointment)**: Orchestrates the scheduling of appointments, integrating with the Doctor and Patient services to validate and retrieve relevant information.

For testing, in addition to unit tests, we have two other projects:

- **[e2e-tests](tests/tests-integration-pms)**: This project aims to create integrated tests using the [Karate](https://github.com/karatelabs/karate) library.
- **[performance-tests](tests/tests-performance-pms)**: This project aims to create performance tests using the [Gatling](https://docs.gatling.io/) library.

Key Technical Features:

- **Database**: Each microservice uses its own dedicated [MariaDB](https://mariadb.com/docs/general-resources/database-theory/introduction-to-relational-databases) schema. Table creation and initial data seeding are handled via Flyway migration scripts located within each service's resources.
- **Service Discovery**: [Netflix Eureka](https://spring.io/projects/spring-cloud-netflix) is used for service registration and discovery, allowing services to locate each other by name rather than hardcoded URLs.
- **API Gateway**: A [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) sits in front of all microservices, handling routing, JWT validation, and load balancing.
- **Communication**: Synchronous communication between services is handled via REST using [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign).
- **Resilience**: Circuit breaking and retry logic are implemented using [Resilience4j](https://resilience4j.readme.io/) to gracefully handle downstream service failures.
- **Observability**: Centralised logging with [Loki](https://grafana.com/docs/loki/latest/), distributed tracing with [Zipkin](https://zipkin.io/), metrics collection with [Prometheus](https://prometheus.io/), and dashboards with [Grafana](https://grafana.com/docs/grafana/latest/).

---

## 🏗️ Architecture

This section presents the **high-level architecture** of the system.

![Alt text](__assets/images/pms-architecture.png?raw=true "Patient Management System Architecture")

---

## 🛠️ Technology Stack

- Language: Java 21
- Framework: Spring Boot 4.x
- Data: Spring Data JPA, MariaDB, Flyway
- Cloud: Spring Cloud OpenFeign, Spring Cloud Gateway, Netflix Eureka, **Spring Cloud Config**
- Security: Spring Security, JWT (JSON Web Tokens)
- Resilience: Resilience4j (Circuit Breaker, Retry)
- API Documentation: OpenAPI (Swagger)
- Containerisation: Docker, Docker Compose
- Logging: Loki, Logback
- Tracing: Zipkin (Micrometer Tracing)
- Monitoring: Prometheus, Grafana
- Performance Testing: Gatling
- End-to-End Tests: Karate framework

---

## 🌐 Infrastructure Services

### 🔀 API Gateway (`infra-api-gateway`)

The API Gateway is the single entry point for all external requests. It runs on port **9094** and is responsible for:

- **Routing** incoming requests to the appropriate downstream microservice, resolved via Eureka service discovery.
- **JWT validation** — every protected route passes through a `JwtValidationGatewayFilterFactory` filter before the request is forwarded. Public routes (e.g. `/v1/auth/**`) bypass this filter.
- **Load balancing** — uses Spring Cloud LoadBalancer to distribute traffic across registered instances of each service.

Key configuration (`application.yml`):

```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

server:
  port: 9094

eureka:
  client:
    fetch-registry: true
    register-with-eureka: true
    serviceUrl:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,metrics
  tracing:
    sampling:
      probability: 1.0
    export:
      zipkin:
        endpoint: http://localhost:9411/api/v2/spans
```

Route definitions are declared programmatically in `RouterConfig.java`. Auth routes under `/v1/auth/**` are public; all other routes require a valid JWT:

```java
// Public — no JWT required
@Bean
public RouterFunction<ServerResponse> authPublicRoutes() {
    return route("auth-service-public")
        .route(RequestPredicates.path("/v1/auth/**"), http())
        .filter(lb("AUTH-SERVICE"))
        .build();
}

// Protected — JWT required
@Bean
public RouterFunction<ServerResponse> doctorRoutes() {
    return route("doctor-service")
        .route(RequestPredicates.path("/v1/doctors/**"), http())
        .filter(lb("DOCTOR-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
}
```

Access the gateway at: **http://localhost:9094**

---

### 🔍 Eureka Discovery Service (`infra-discovery-service`)

Netflix Eureka is the service registry for the PMS ecosystem. All microservices and the API Gateway register themselves with Eureka on startup, and use it to resolve service addresses at runtime — no hardcoded host/port configuration is needed in production.

Key configuration (`application.yml`):

```yaml
spring:
  application:
    name: discovery-service

server:
  port: 8761

eureka:
  client:
    fetch-registry: false
    register-with-eureka: false   # The server does not register itself

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, metrics
  tracing:
    sampling:
      probability: 1.0
```

Access the Eureka dashboard at: **http://localhost:8761**

![Alt text](__assets/images/eureka.png?raw=true "Eureka Serice Discovery")

---

### ⚙️ Config Server (`infra-config-server`)

The Config Server is the **centralised configuration hub** for all microservices. It serves YAML configuration files from a Git repository, giving every service a single auditable source of truth for its settings — independent of the application's own source code.

- Runs on port **8888**
- Registers with Eureka as `config-server`
- Exposes configuration at: `http://localhost:8888/{service}/{profile}`
- Accessible through the gateway at: `http://localhost:9094/config/{service}/{profile}`

#### How it works

At startup each microservice contacts the Config Server (before the application context is fully refreshed) and downloads configuration for its `spring.application.name` + active profile. The downloaded properties are merged with the service's own `application.yml`, with the Config Server values taking the highest precedence.

```
Service startup
  └─► contacts Config Server (http://config-server:8888)
        └─► fetches {service}/application.yml
        └─► fetches {service}/application-{profile}.yml
              └─► values merged into service's Environment
```

#### Configuration

```yaml
spring:
  config:
    import: "optional:configserver:http://localhost:8888"
```

The `optional:` prefix means the service **starts normally** if the Config Server is temporarily unreachable — it falls back to its local `application.yml`. Remove `optional:` to make the Config Server a hard dependency (the service refuses to start if it cannot reach the server).

#### Live refresh (without restart)

To apply a configuration change after pushing to the Git repository:

```bash
# Refresh a single service
curl -X POST http://localhost:8083/actuator/refresh

# Or through the gateway
curl -X POST http://localhost:9094/config/actuator/refresh
```

Beans annotated with `@RefreshScope` or `@ConfigurationProperties` (like `AppointmentProperties`) will pick up the new values immediately.

---

## 🗄️ Database

Each microservice persists data in its own isolated MariaDB database schema. This ensures data encapsulation and loose coupling between services.

![Alt text](__assets/images/pms-der.png?raw=true "Patient Management DER")

---

## 📚 API List

All requests flow through the API Gateway at `http://localhost:9094`. The endpoints below are the paths exposed at the gateway level.

### 🔐 Authentication Service

#### Auth (`/v1/auth`) — Public, no JWT required

| Method | Endpoint          | Description                              |
|--------|-------------------|------------------------------------------|
| POST   | /v1/auth/login    | Login with email/password, returns JWT   |

#### User Management (`/v1/users`) — Requires JWT + `HEALTHCARE_ADMIN` role

| Method | Endpoint                       | Description                                      |
|--------|--------------------------------|--------------------------------------------------|
| POST   | /v1/users                      | Create a user account (doctor or patient)        |
| DELETE | /v1/users/domain/{domainId}    | Disable a user account by domain entity ID       |

### 🩺 Doctor Service

| Method | Endpoint           | Description          |
|--------|--------------------|----------------------|
| POST   | /v1/doctors        | Create a doctor      |
| GET    | /v1/doctors        | List doctors (paged) |
| GET    | /v1/doctors/{id}   | Get doctor by ID     |
| PUT    | /v1/doctors/{id}   | Update doctor        |
| DELETE | /v1/doctors/{id}   | Delete doctor        |

### 🧑 Patient Service

| Method | Endpoint            | Description           |
|--------|---------------------|-----------------------|
| POST   | /v1/patients        | Create a patient      |
| GET    | /v1/patients        | List patients (paged) |
| GET    | /v1/patients/{id}   | Get patient by ID     |
| PUT    | /v1/patients/{id}   | Update patient        |
| DELETE | /v1/patients/{id}   | Delete patient        |

### 📅 Appointment Service

| Method | Endpoint                                         | Description                    |
|--------|--------------------------------------------------|--------------------------------|
| POST   | /v1/patients/{patientId}/appointments            | Create an appointment          |
| GET    | /v1/patients/{patientId}/appointments            | List appointments for patient  |
| GET    | /v1/patients/{patientId}/appointments/{id}       | Get appointment by ID          |
| PUT    | /v1/patients/{patientId}/appointments/{id}       | Update an appointment          |
| DELETE | /v1/patients/{patientId}/appointments/{id}       | Delete an appointment          |
| PATCH  | /v1/patients/{patientId}/appointments/{id}/cancel | Cancel an appointment         |
| GET    | /v1/appointments                                 | List all appointments (admin)  |

---

## 📘 Documentation & Testing

### OpenAPI / Swagger
Once services are running, you can access the interactive API documentation:

- Patient Service: http://localhost:8081/swagger-ui.html
- Doctor Service: http://localhost:8082/swagger-ui.html
- Appointment Service: http://localhost:8083/swagger-ui.html
- Authentication Service: http://localhost:8084/swagger-ui.html

![Alt text](__assets/images/openapi-documentation.png?raw=true "OpenAPI Documentation Example")

### Postman Collection

A **Postman collection** is provided to test all APIs.
- Location: `/documentation/postman/PMS.postman_collection.json`

---

## 🔍 Centralized Logging

All microservices ship their logs to a central **Loki** instance, which is then visualised through **Grafana**. Each log line is tagged with the application name, trace ID, and span ID, making it straightforward to correlate logs with distributed traces.

### Loki

[Grafana Loki](https://grafana.com/docs/loki/latest/) is a horizontally scalable log aggregation system. Unlike traditional logging stacks, Loki indexes only the metadata labels attached to log lines (such as `app`, `level`, and `traceID`) rather than the full log content. This makes it lightweight and cost-effective while still enabling powerful label-based filtering in Grafana.

Each microservice uses the `loki4j` Logback appender to push logs directly to Loki. The configuration is defined in `logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <springProperty scope="context" name="appName" source="spring.application.name"/>

  <property name="LOG_PATTERN"
            value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [${appName},%X{traceId:-},%X{spanId:-}] [%thread] %logger{36} : %msg%n"/>

  <appender name="PMS_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>

  <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
      <url>http://loki:3100/loki/api/v1/push</url>
    </http>
    <format>
      <label>
        <pattern>app=${appName},host=${HOSTNAME},traceID=%X{traceId:-NONE},level=%level</pattern>
        <readMarkers>true</readMarkers>
      </label>
      <message>
        <pattern>${LOG_PATTERN}</pattern>
      </message>
    </format>
  </appender>

  <springProfile name="docker">
    <root level="INFO">
      <appender-ref ref="PMS_CONSOLE"/>
      <appender-ref ref="LOKI"/>
    </root>
  </springProfile>

  <springProfile name="!docker">
    <root level="INFO">
      <appender-ref ref="PMS_CONSOLE"/>
    </root>
  </springProfile>

</configuration>
```

- Loki is accessible at: **http://localhost:3100**
- Logs are queried through the Grafana Explore view using LogQL.

---

## 🕵️ Distributed Tracing

### Zipkin

[Zipkin](https://zipkin.io/) is a distributed tracing system that helps track the lifecycle of a request as it travels through multiple microservices. Every request that enters the system through the API Gateway is assigned a **trace ID** and **span ID** via [Micrometer Tracing](https://micrometer.io/docs/tracing). These IDs are propagated across service-to-service calls and included in log output, making it possible to reconstruct the full call chain from a single trace ID.

Each microservice is configured to export traces to Zipkin:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0       # Sample 100% of requests
    export:
      enabled: true
      zipkin:
        endpoint: http://zipkin:9411/api/v2/spans
```

With `probability: 1.0` every request is traced. In a high-traffic production environment this value would typically be reduced (e.g. `0.1` for 10%) to control the volume of trace data.

- Access Zipkin at: **http://localhost:9411**
- Use the trace ID visible in log output to search for the full distributed trace in the Zipkin UI.

![Alt text](__assets/images/zipkin.png?raw=true "Zipkin Dependencies graph")

---

## 📊 Monitoring

Monitoring ensures the health and performance of the microservices.

### Prometheus

**Prometheus** is an open-source systems monitoring and alerting toolkit. It scrapes and stores metrics as time series data. Each microservice exposes a `/actuator/prometheus` endpoint, and the `prometheus.yml` file defines the scrape targets for all services in the system — including the gateway, discovery service, and all four microservices.

Each microservice exposes metrics using the following configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics,info,health,prometheus
  endpoint:
    metrics:
      access: read_only
    prometheus:
      access: read_only
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram.http.server.requests: true
      percentiles.http.server.requests: 0.5, 0.9, 0.95, 0.99, 0.999
      sla.http.server.requests: 500ms, 2000ms
```

Access Prometheus at **http://localhost:9090**

![Alt text](__assets/images/prometheus-query.png?raw=true "Prometheus Query Example")

---

### Grafana

**Grafana** is an open-source analytics and interactive visualization platform. It connects to both Prometheus (for metrics) and Loki (for logs) to provide unified dashboards for the entire system.

- Access: http://localhost:3000
- Username: `admin`
- Password: `admin`

![Alt text](__assets/images/grafana-dashboard.png?raw=true "Grafana Dashboard Example")

---

## ⚙️ Externalised Configuration — Appointment Duration

The `AppointmentService` uses a configurable duration value (in minutes) for calculating `endTime` and `duration` on every appointment. This value is no longer hardcoded — it is read from the Config Server at startup and can be changed live without redeploying.

### Configuration property

In the config repository at `appointment-service/application.yml`:

```yaml
pms:
  appointment:
    duration-minutes: 60   # change to 30, 45, 90, etc.
```

### How it works in code

`AppointmentProperties` is a `@ConfigurationProperties` bean bound to the `pms.appointment` prefix:

```java
@Component
@ConfigurationProperties(prefix = "pms.appointment")
public class AppointmentProperties {
    private int durationMinutes = 60;   // safe default if config server is unreachable
    // getters / setters
}
```

`AppointmentService` injects this bean and uses the value in both `insert()` and `update()`:

```java
int durationMinutes = appointmentProperties.getDurationMinutes();

appointment.setDuration(durationMinutes);
appointment.setEndTime(appointment.getStartTime().plusMinutes(durationMinutes));
```

### Changing the duration at runtime

```bash
# 1. Edit appointment-service/application.yml in the config repository
#    (change duration-minutes to the desired value, e.g. 30)

# 2. Commit and push
git commit -am "chore: set appointment duration to 30 minutes"
git push

# 3. Trigger a live refresh — no restart required
curl -X POST http://localhost:8083/actuator/refresh
```

---

## 🛡️ Resilience

The Appointment Service communicates synchronously with both the Doctor Service and the Patient Service via Feign clients. To prevent cascading failures when a downstream service is slow or unavailable, the system uses **Resilience4j** for circuit breaking and automatic retries.

### Why Resilience4j?

In a microservices system, a slow or failing dependency can cause threads to pile up and exhaust the thread pool of the calling service — a cascade failure. Resilience4j addresses this with two complementary patterns:

- **Retry** — automatically retries a failed call a configurable number of times with a delay, handling transient network errors.
- **Circuit Breaker** — tracks the failure rate of calls over a sliding window. If failures exceed a threshold, the circuit opens and subsequent calls fail fast (without even attempting the network call) until the downstream recovers.

### Two Fallback Strategies

The Appointment Service applies two different fallback strategies depending on whether the call is used for a **read** or a **write**:

- **Soft fallback (reads)** — if the Doctor or Patient service is unavailable while fetching appointment details, the service degrades gracefully and returns a partial response (ID only), rather than failing the entire request.
- **Hard fallback (writes)** — if the Doctor or Patient service is unavailable during appointment creation or update, the write is blocked and an HTTP 503 is returned. This prevents saving an appointment with an unverified doctor or patient, which would be data corruption.

### Configuration (`application.yml`)

```yaml
resilience4j:

  retry:
    instances:
      doctorClient:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
      patientClient:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException

  circuitbreaker:
    instances:
      doctorClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true
      patientClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true
```

**Configuration explained:**

| Setting | Value | Meaning |
|---|---|---|
| `max-attempts` | 3 | Try the call up to 3 times before giving up |
| `wait-duration` | 500ms | Wait 500 ms between retry attempts |
| `sliding-window-size` | 10 | Track the last 10 calls to measure failure rate |
| `minimum-number-of-calls` | 5 | Require at least 5 calls before the circuit can open |
| `failure-rate-threshold` | 50 | Open the circuit when ≥ 50% of calls fail |
| `wait-duration-in-open-state` | 10s | Keep the circuit open for 10 s before trying again |
| `permitted-number-of-calls-in-half-open-state` | 5 | Allow 5 probe calls when transitioning from OPEN to CLOSED |
| `automatic-transition-from-open-to-half-open-enabled` | true | Automatically move to HALF-OPEN after the wait duration, without needing a new request to trigger it |

### Code — `AppointmentService.java`

```java
private static final String DOCTOR_CB = "doctorClient";
private static final String PATIENT_CB = "patientClient";

// ── Soft fetch — used by reads, degrades gracefully ──────────────────────
@Retry(name = DOCTOR_CB, fallbackMethod = "fetchDoctorFallback")
@CircuitBreaker(name = DOCTOR_CB, fallbackMethod = "fetchDoctorFallback")
public DoctorResponse fetchDoctor(Long doctorId) {
    log.debug("Fetching doctor id={}", doctorId);
    return doctorClient.findById(doctorId);
}

@Retry(name = PATIENT_CB, fallbackMethod = "fetchPatientFallback")
@CircuitBreaker(name = PATIENT_CB, fallbackMethod = "fetchPatientFallback")
public PatientResponse fetchPatient(Long patientId) {
    log.debug("Fetching patient id={}", patientId);
    return patientClient.findById(patientId);
}

// ── Hard validate — used by writes, blocks on failure ────────────────────
@Retry(name = DOCTOR_CB, fallbackMethod = "validateDoctorFallback")
@CircuitBreaker(name = DOCTOR_CB, fallbackMethod = "validateDoctorFallback")
public DoctorResponse validateDoctor(Long doctorId) {
    log.debug("Validating doctor id={}", doctorId);
    return doctorClient.findById(doctorId);
}

// ── Soft fallback — returns id-only response ──────────────────────────────
public DoctorResponse fetchDoctorFallback(Long doctorId, Throwable ex) {
    log.warn("Doctor service unavailable for doctorId={}. Falling back to id-only. Cause: {}",
        doctorId, ex.getMessage());
    var fallback = new DoctorResponse();
    fallback.setId(doctorId);
    return fallback;
}

// ── Hard fallback — blocks the write with HTTP 503 ────────────────────────
public DoctorResponse validateDoctorFallback(Long doctorId, Throwable ex) {
    log.error("Doctor service unavailable for doctorId={}. Blocking write. Cause: {}",
        doctorId, ex.getMessage());
    throw new ServiceUnavailableException(
        "Doctor service is currently unavailable. Cannot validate doctor id %d. Please try again later."
            .formatted(doctorId));
}
```

The `@Retry` annotation is evaluated first — it attempts the call up to `max-attempts` times. Only after all retries are exhausted does the `@CircuitBreaker` fallback fire. This ordering ensures transient failures are recovered automatically without opening the circuit prematurely.

---

## 🧪 Tests

The project includes three types of tests: unit tests, integration tests using Karate, and performance tests using Gatling.

### 🧩 Unit tests

Execute the unit tests using the command below:
```bash
mvn test 
```

### 🔗 Integration tests

Karate is a testing framework designed for API testing, offering a simplified and expressive syntax to create and execute tests for web services. It integrates seamlessly with HTTP, allowing for straightforward validation of RESTful APIs by enabling testers to define requests and assertions in a readable, concise manner.

Execute the karate tests using the command below:
```bash
mvn test -Dkarate.env=local
```

Results can be found in the [tests/tests-integration-pms/target/karate-reports]() folder

![Alt text](__assets/images/karate-results_1.png?raw=true "Karate Test Result")

### ⚡ Performance tests

Gatling is a powerful open-source load testing tool designed to analyze and measure the performance of web applications. It allows developers and testers to simulate a large number of users interacting with a system, providing detailed reports and metrics on response times, throughput, and error rates.

Execute the performance tests using the command below:

```bash
mvn gatling:test  \
          -Dgatling.simulationClass=com.pms.performance.simulation.PmsLoadSimulation  \
          -DpatientUrl=http://localhost:8081  \
          -DdoctorUrl=http://localhost:8082  \
          -DappointmentUrl=http://localhost:8083 \
          -DauthUrl=http://localhost:8084  \
          -Dusers=5  \
          -DdurationMinutes=30
```

**Parameters:**
- `simulationClass`: Gatling simulation class
- `patientUrl`: Patient service URL
- `doctorUrl`: Doctor service URL
- `appointmentUrl`: Appointment service URL
- `authUrl`: Authentication service URL
- `users`: Number of virtual users
- `durationMinutes`: Test duration

![Alt text](__assets/images/gatling-results.png?raw=true "Gatling Test Results Example")

---

## 🚀 Deployment

### 📦 Docker Compose

**Docker Compose** is a tool for defining and running multi-container Docker applications. It is the deployment strategy used by this project for local development and containerised environments.

The `docker-compose.yml` at the root of the project defines and orchestrates the full system. It includes configuration for:

- **Database** — MariaDB with initialisation scripts
- **Discovery** — Eureka Server
- **Gateway** — API Gateway
- **Microservices** — Auth, Patient, Doctor, Appointment
- **Loki** — Log aggregation
- **Zipkin** — Distributed tracing
- **Prometheus** — Metrics scraping
- **Grafana** — Dashboards and visualisation

![Alt text](__assets/images/docker-compose-folder-structure.png?raw=true "Docker Compose Folder Structure")

Commands:

```bash
# 1. Copy and configure environment variables (including CONFIG_GIT_URI)
cp .env.example .env
# Edit .env — set JWT_SECRET and CONFIG_GIT_URI

# Start all services
docker compose -f docker-compose.yml up -d

# Stop and remove containers
docker compose -f docker-compose.yml down
```

---

## 🔨 Build Project & Running Locally

To build the entire project using Maven:

```bash
mvn clean install
```

### Running Locally
To run a microservice locally, navigate to its directory and use:

```bash
cd ms-doctor
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Running as a Container

Each microservice includes a **Dockerfile** to build a container image. Microservices use [Distroless](https://github.com/GoogleContainerTools/distroless) base images for a minimal, secure runtime with no shell or package manager. Infrastructure services (gateway and discovery) use `eclipse-temurin:21-jre-alpine`.

**Microservice Dockerfile (Distroless):**

```dockerfile
FROM gcr.io/distroless/java21-debian13

ADD target/appointment-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**Infrastructure service Dockerfile (Alpine JRE):**

```dockerfile
FROM eclipse-temurin:21-jre-alpine

ADD target/api-gateway-service.jar app.jar

EXPOSE 9094

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

The Distroless image is used for microservices because it contains only the JRE and the application itself — no shell, no package manager, and a significantly smaller attack surface. The Alpine-based image is used for infrastructure services where the slightly larger image size is acceptable and tooling (such as `curl` for health checks in the discovery service) may be required.

Execute the command below to run a container individually (requires database and Eureka to be running):

```bash
docker run -e SPRING_PROFILES_ACTIVE=docker -p 8083:8083 pms/appointment-service
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://mit-license.org/) file for details.

---
