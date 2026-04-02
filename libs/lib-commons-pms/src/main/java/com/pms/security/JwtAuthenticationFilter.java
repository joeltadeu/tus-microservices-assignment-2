package com.pms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Shared stateless JWT validation filter — lives in lib-commons-pms so every microservice reuses it
 * without duplicating code.
 *
 * <p>What it does: 1. Reads the Bearer token from the Authorization header. 2. Validates the HS512
 * signature and expiry using the shared secret. 3. Extracts roles from the "roles" claim and sets
 * them as granted authorities. 4. Stores userId (sub) and domainId as principal/credentials on the
 * authentication object — controllers can use these for ownership checks.
 *
 * <p>No network call to ms-auth is made — validation is purely cryptographic.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CLAIM_ROLES = "roles";
  private static final String CLAIM_DOMAIN = "domainId";

  private final SecretKey signingKey;

  public JwtAuthenticationFilter(String jwtSecret) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    String token = header.substring(BEARER_PREFIX.length());

    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

      List<String> roles = claims.get(CLAIM_ROLES, List.class);
      var authorities =
          roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

      // principal  = userId string  (use in @PreAuthorize for ownership checks)
      // credentials = domainId Long (doctor.id or patient.id; null for admin)
      var auth =
          new UsernamePasswordAuthenticationToken(
              claims.getSubject(), claims.get(CLAIM_DOMAIN, Long.class), authorities);

      SecurityContextHolder.getContext().setAuthentication(auth);

    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT validation failed: {}", e.getMessage());
      // Leave SecurityContext empty → Spring Security returns 401 automatically
    }

    chain.doFilter(request, response);
  }
}
