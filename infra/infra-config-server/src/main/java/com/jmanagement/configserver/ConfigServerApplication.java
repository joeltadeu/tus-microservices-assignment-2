package com.jmanagement.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralised Spring Cloud Config Server.
 *
 * <p>All microservices in the PMS platform fetch their configuration from this server at startup
 * (and optionally at runtime via {@code /actuator/refresh}). Configuration files are stored in a
 * dedicated Git repository, giving a single auditable source of truth for every environment.
 *
 * <p>The server registers itself with Eureka so that client services can discover it by name
 * ({@code config-server}) rather than relying on a hardcoded host and port.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
