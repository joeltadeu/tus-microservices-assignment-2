package com.jmanagement.apigateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import com.jmanagement.apigateway.filter.JwtValidationGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class RouterConfig {

  private final JwtValidationGatewayFilterFactory jwtFilter;

  public RouterConfig(JwtValidationGatewayFilterFactory jwtFilter) {
    this.jwtFilter = jwtFilter;
  }

  // ── Config Server — read-only config inspection (no JWT — infra/ops use) ──
  // GET /config/{service}/{profile}  → returns the raw YAML served to that service.
  // The /config prefix is stripped before forwarding so the Config Server
  // receives /{service}/{profile} (not /config/{service}/{profile}).
  // POST is intentionally excluded: refreshing the Config Server itself has no
  // effect — use the per-service actuator routes below instead.
  @Bean
  public RouterFunction<ServerResponse> configServerRoutes() {
    return route("config-server")
        .route(
            RequestPredicates.path("/config/**").and(RequestPredicates.method(HttpMethod.GET)),
            http())
        .before(rewritePath("/config/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("CONFIG-SERVER"))
        .build();
  }

  // ── Per-service actuator proxy — no JWT (internal ops use) ──────────────
  //
  // The gateway mounts its OWN actuator at /actuator/**, so any route under
  // that prefix is intercepted by the gateway itself and never reaches these
  // router beans. To avoid that collision the proxy routes put the service
  // name first: /{service}/actuator/**.
  //
  // Usage:
  //   POST /appointment/actuator/refresh  → appointment-service /actuator/refresh
  //   POST /doctor/actuator/refresh       → doctor-service      /actuator/refresh
  //   POST /patient/actuator/refresh      → patient-service     /actuator/refresh
  //   POST /auth/actuator/refresh         → auth-service        /actuator/refresh
  //   GET  /appointment/actuator/health   → appointment-service /actuator/health
  //
  // The /{service} prefix is stripped before forwarding.
  @Bean
  public RouterFunction<ServerResponse> appointmentActuatorRoutes() {
    return route("appointment-service-actuator")
        .route(
            RequestPredicates.path("/appointment/actuator/**")
                .and(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)),
            http())
        .before(rewritePath("/appointment/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("APPOINTMENT-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> doctorActuatorRoutes() {
    return route("doctor-service-actuator")
        .route(
            RequestPredicates.path("/doctor/actuator/**")
                .and(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)),
            http())
        .before(rewritePath("/doctor/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("DOCTOR-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> patientActuatorRoutes() {
    return route("patient-service-actuator")
        .route(
            RequestPredicates.path("/patient/actuator/**")
                .and(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)),
            http())
        .before(rewritePath("/patient/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("PATIENT-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> authActuatorRoutes() {
    return route("auth-service-actuator")
        .route(
            RequestPredicates.path("/auth/actuator/**")
                .and(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)),
            http())
        .before(rewritePath("/auth/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("AUTH-SERVICE"))
        .build();
  }

  // ── Per-service Swagger UI proxy — no JWT (dev/docs use) ─────────────────
  //
  // Swagger UI makes several internal requests to /swagger-ui/**, /api-docs/**
  // and /swagger-ui/index.html. All of them are covered by the same wildcard
  // route. The /{service} prefix is stripped before forwarding so each service
  // receives the exact paths it expects.
  //
  // Usage:
  //   GET :9094/appointment/swagger-ui/index.html
  //   GET :9094/doctor/swagger-ui/index.html
  //   GET :9094/patient/swagger-ui/index.html
  //   GET :9094/auth/swagger-ui/index.html
  @Bean
  public RouterFunction<ServerResponse> appointmentSwaggerRoutes() {
    return route("appointment-service-swagger")
        .route(
            RequestPredicates.path("/appointment/swagger-ui/**")
                .or(RequestPredicates.path("/appointment/api-docs/**")),
            http())
        .before(rewritePath("/appointment/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("APPOINTMENT-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> doctorSwaggerRoutes() {
    return route("doctor-service-swagger")
        .route(
            RequestPredicates.path("/doctor/swagger-ui/**")
                .or(RequestPredicates.path("/doctor/api-docs/**")),
            http())
        .before(rewritePath("/doctor/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("DOCTOR-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> patientSwaggerRoutes() {
    return route("patient-service-swagger")
        .route(
            RequestPredicates.path("/patient/swagger-ui/**")
                .or(RequestPredicates.path("/patient/api-docs/**")),
            http())
        .before(rewritePath("/patient/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("PATIENT-SERVICE"))
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> authSwaggerRoutes() {
    return route("auth-service-swagger")
        .route(
            RequestPredicates.path("/auth/swagger-ui/**")
                .or(RequestPredicates.path("/auth/api-docs/**")),
            http())
        .before(rewritePath("/auth/(?<remaining>.*)", "/${remaining}"))
        .filter(lb("AUTH-SERVICE"))
        .build();
  }

  // ── Auth service — public (no JWT required) ──────────────────────────────
  @Bean
  public RouterFunction<ServerResponse> authPublicRoutes() {
    return route("auth-service-public")
        .route(RequestPredicates.path("/v1/auth/**"), http())
        .filter(lb("AUTH-SERVICE"))
        .build();
  }

  // ── Auth service — user management (JWT required) ────────────────────────
  @Bean
  public RouterFunction<ServerResponse> authUserRoutes() {
    return route("auth-service-users")
        .route(RequestPredicates.path("/v1/users/**"), http())
        .filter(lb("AUTH-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
  }

  // ── Appointment service — must be before patient routes (more specific) ──
  @Bean
  public RouterFunction<ServerResponse> appointmentRoutes() {
    return route("appointment-service")
        .route(RequestPredicates.path("/v1/patients/*/appointments/**"), http())
        .filter(lb("APPOINTMENT-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
  }

  // ── Appointment service — admin only, list all appointments ──────────────
  @Bean
  public RouterFunction<ServerResponse> appointmentAdminRoutes() {
    return route("appointment-service-admin")
        .route(RequestPredicates.path("/v1/appointments/**"), http())
        .filter(lb("APPOINTMENT-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
  }

  // ── Doctor service ────────────────────────────────────────────────────────
  @Bean
  public RouterFunction<ServerResponse> doctorRoutes() {
    return route("doctor-service")
        .route(RequestPredicates.path("/v1/doctors/**"), http())
        .filter(lb("DOCTOR-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
  }

  // ── Patient service — after appointment routes (less specific) ───────────
  @Bean
  public RouterFunction<ServerResponse> patientRoutes() {
    return route("patient-service")
        .route(
            RequestPredicates.path("/v1/patients").or(RequestPredicates.path("/v1/patients/{id}")),
            http())
        .filter(lb("PATIENT-SERVICE"))
        .filter(jwtFilter.apply())
        .build();
  }
}
