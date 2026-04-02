package com.pms.appointment.config;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI testModuleOpenAPI() {
    return new OpenAPI()
        .info(
            new io.swagger.v3.oas.models.info.Info()
                .title("Appointment Service API")
                .version("1.0.0")
                .description(
                    """
                                                        The Appointment Service API handles the scheduling and tracking of medical consultations. It integrates with the Doctor and Patient services to validate participants, enabling the creation of appointments, retrieval of consultation details, and monitoring of appointment statuses.
                                                        """)
                .contact(
                    new io.swagger.v3.oas.models.info.Contact()
                        .name("Joel Silva")
                        .url("https://github.com/joeltadeu")
                        .email("joeltadeu@gmail.com"))
                .license(
                    new io.swagger.v3.oas.models.info.License()
                        .name("MIT License")
                        .url("https://mit-license.org/")))
        .servers(
            List.of(
                new io.swagger.v3.oas.models.servers.Server()
                    .url("http://localhost:8083")
                    .description("Local Development Server")));
  }
}
