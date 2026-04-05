package com.pms.patient.config;

import com.pms.security.FeignJwtInterceptor;
import com.pms.security.JwtAuthenticationFilter;
import feign.Logger;
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
  public Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL;
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      @Value("${pms.jwt.secret}") String secret) {
    return new JwtAuthenticationFilter(secret);
  }

  /**
   * Propagates the inbound admin JWT to the outbound AuthClient Feign call so ms-auth can verify
   * the caller has HEALTHCARE_ADMIN role.
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

                    // ── Specific rules first — Spring Security is first-match-wins ──

                    // List all patients: admin only
                    .requestMatchers(HttpMethod.GET, "/v1/patients")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Create patient: admin only
                    .requestMatchers(HttpMethod.POST, "/v1/patients")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Delete patient: admin only
                    .requestMatchers(HttpMethod.DELETE, "/v1/patients/{id}")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Update own patient record: patient or admin
                    .requestMatchers(HttpMethod.PUT, "/v1/patients/{id}")
                    .hasAnyRole("PATIENT", "HEALTHCARE_ADMIN")
                    // Get patient by id: any authenticated user
                    // (needed by appointment enrichment from ms-appointment)
                    .requestMatchers(HttpMethod.GET, "/v1/patients/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
