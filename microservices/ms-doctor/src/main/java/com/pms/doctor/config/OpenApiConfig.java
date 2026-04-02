package com.pms.doctor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI testModuleOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Doctor Service API")
                .version("1.0.0")
                .description(
                    """
                                       The Doctor Service API is responsible for managing the lifecycle of doctor profiles within the system. It provides endpoints to create, retrieve, update, and delete medical staff records, including essential details such as specialties, departments, and contact information.
                                    """)
                .contact(
                    new Contact()
                        .name("Joel Silva")
                        .url("https://github.com/joeltadeu")
                        .email("joeltadeu@gmail.com"))
                .license(new License().name("MIT License").url("https://mit-license.org/")))
        .servers(
            List.of(
                new Server().url("http://localhost:8082").description("Local Development Server")));
  }
}
