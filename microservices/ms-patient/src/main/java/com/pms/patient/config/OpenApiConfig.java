package com.pms.patient.config;

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
                .title("Patient Service API")
                .version("1.0.0")
                .description(
                    """
                                                           The Patient Service API manages comprehensive patient records, handling personal and demographic information. It provides endpoints to register new patients, retrieve individual profiles, update existing records, and manage patient data securely.
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
                    .url("http://localhost:8081")
                    .description("Local Development Server")));
  }
}
