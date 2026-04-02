package com.jmanagement.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Named gateway filter "JwtValidation".
 *
 * <p>Spring Cloud Gateway (MVC) discovers named filters by convention: a bean whose class name ends
 * in {@code GatewayFilterFactory} and whose simple name prefix matches the filter name used in the
 * route YAML.
 *
 * <p>Validates {@code Authorization: Bearer <token>}, returns 401 on failure, and forwards the
 * authenticated subject as {@code X-Auth-User} to downstream services.
 */
@Component
public class JwtValidationGatewayFilterFactory extends SimpleFilterSupplier {

  private static final Logger log =
      LoggerFactory.getLogger(JwtValidationGatewayFilterFactory.class);

  private final SecretKey signingKey;

  public JwtValidationGatewayFilterFactory(@Value("${pms.jwt.secret}") String secret) {
    super(JwtValidationGatewayFilterFactory.class);
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Produces the filter function applied to every route that declares {@code - name:
   * JwtValidation}.
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> apply() {
    return (request, next) -> {
      String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("JWT filter: missing/malformed Authorization header — {}", request.uri());
        return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
      }

      String token = authHeader.substring(7);

      try {
        Claims claims =
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

        // Propagate the authenticated subject downstream
        ServerRequest enriched =
            ServerRequest.from(request).header("X-Auth-User", claims.getSubject()).build();

        return next.handle(enriched);

      } catch (JwtException | IllegalArgumentException ex) {
        log.warn("JWT filter: invalid token for {} — {}", request.uri(), ex.getMessage());
        return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
      }
    };
  }
}
