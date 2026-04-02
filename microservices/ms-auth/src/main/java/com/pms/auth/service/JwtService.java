package com.pms.auth.service;

import com.pms.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

  static final String CLAIM_ROLES = "roles";
  static final String CLAIM_DOMAIN_ID = "domainId";

  private final SecretKey signingKey;
  private final long accessTokenExpiryMs;

  public JwtService(
      @Value("${pms.jwt.secret}") String secret,
      @Value("${pms.jwt.access-token-expiry-minutes:15}") int expiryMinutes) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiryMs = (long) expiryMinutes * 60 * 1000;
  }

  /**
   * Builds a signed HS512 JWT. Claims carried: sub (userId), roles (list), domainId (doctor/patient
   * row id).
   */
  public String generateAccessToken(User user) {
    List<String> roles =
        user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList());

    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim(CLAIM_ROLES, roles)
        .claim(CLAIM_DOMAIN_ID, user.getDomainId())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
        .signWith(signingKey)
        .compact();
  }

  /** Validates signature + expiry and returns the parsed claims. Throws JwtException if invalid. */
  public Claims validateAndParse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public boolean isValid(String token) {
    try {
      validateAndParse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT validation failed: {}", e.getMessage());
      return false;
    }
  }

  public long getAccessTokenExpiryMs() {
    return accessTokenExpiryMs;
  }
}
