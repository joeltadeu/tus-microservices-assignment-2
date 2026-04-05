package com.pms.doctor.config;

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
                        "/api-docs/**",
                        "/scalar/**",
                        "/swagger-ui",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**")
                    .permitAll()

                    // ── Specific rules first — Spring Security is first-match-wins ──

                    // Create doctor: admin only
                    .requestMatchers(HttpMethod.POST, "/v1/doctors")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Delete doctor: admin only
                    .requestMatchers(HttpMethod.DELETE, "/v1/doctors/{id}")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Update own doctor record: doctor or admin
                    .requestMatchers(HttpMethod.PUT, "/v1/doctors/{id}")
                    .hasAnyRole("DOCTOR", "HEALTHCARE_ADMIN")
                    // List all doctors: admin only
                    .requestMatchers(HttpMethod.GET, "/v1/doctors")
                    .hasRole("HEALTHCARE_ADMIN")
                    // Get doctor by id: any authenticated user
                    // (needed by appointment enrichment from ms-appointment)
                    .requestMatchers(HttpMethod.GET, "/v1/doctors/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
