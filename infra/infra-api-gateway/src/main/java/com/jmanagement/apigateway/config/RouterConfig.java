package com.jmanagement.apigateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import com.jmanagement.apigateway.filter.JwtValidationGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class RouterConfig {

  private final JwtValidationGatewayFilterFactory jwtFilter;

  public RouterConfig(JwtValidationGatewayFilterFactory jwtFilter) {
    this.jwtFilter = jwtFilter;
  }

  // ── Config Server — actuator access only (no JWT — infra/internal use) ────
  @Bean
  public RouterFunction<ServerResponse> configServerRoutes() {
    return route("config-server")
        .route(RequestPredicates.path("/config/**"), http())
        .filter(lb("CONFIG-SERVER"))
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
