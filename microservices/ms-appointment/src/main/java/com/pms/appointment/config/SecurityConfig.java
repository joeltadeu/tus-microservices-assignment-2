package com.pms.appointment.config;

import com.pms.security.FeignJwtInterceptor;
import com.pms.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      @Value("${pms.jwt.secret}") String secret) {
    return new JwtAuthenticationFilter(secret);
  }

  /**
   * Registers the Feign interceptor that propagates the inbound JWT to outbound calls to ms-doctor
   * and ms-patient automatically.
   */
  @Bean
  public FeignJwtInterceptor feignJwtInterceptor() {
    return new FeignJwtInterceptor();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
      throws Exception {

    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers(
                        "/api-docs/**", "/scalar/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()

                    // Patients list + read their own appointments
                    .requestMatchers(HttpMethod.GET, "/v1/patients/*/appointments/**")
                    .hasAnyRole("PATIENT", "HEALTHCARE_ADMIN")

                    // Patients cancel their own scheduled appointments
                    .requestMatchers(HttpMethod.POST, "/v1/patients/*/appointments/*/cancel")
                    .hasAnyRole("PATIENT", "HEALTHCARE_ADMIN")

                    // Patients create appointments; admin also can
                    .requestMatchers(HttpMethod.POST, "/v1/patients/*/appointments")
                    .hasAnyRole("PATIENT", "HEALTHCARE_ADMIN")

                    // Patients update their own appointments; admin also can
                    .requestMatchers(HttpMethod.PUT, "/v1/patients/*/appointments/*")
                    .hasAnyRole("PATIENT", "HEALTHCARE_ADMIN")

                    // List all doctors: admin only
                    .requestMatchers(HttpMethod.GET, "/v1/doctors")
                    .hasRole("HEALTHCARE_ADMIN")

                    // Only admin can hard-delete
                    .requestMatchers(HttpMethod.DELETE, "/v1/patients/*/appointments/*")
                    .hasRole("HEALTHCARE_ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
