package com.pms.auth.service;

import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.RefreshRequest;
import com.pms.auth.dto.TokenResponse;
import com.pms.auth.model.RefreshToken;
import com.pms.auth.model.User;
import com.pms.auth.repository.RefreshTokenRepository;
import com.pms.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @Value("${pms.jwt.refresh-token-expiry-days:7}")
  private int refreshExpiryDays;

  @Transactional
  public TokenResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!user.getEnabled()) {
      throw new BadCredentialsException("Account is disabled");
    }
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    // Single-session policy: revoke any existing refresh tokens for this user
    refreshTokenRepository.revokeAllByUserId(user.getId());

    return buildTokenResponse(user);
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) {
    String hash = sha256Hex(request.getRefreshToken());
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

    if (stored.getRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BadCredentialsException("Refresh token expired or revoked");
    }

    // Rotate: revoke consumed token, issue a fresh pair
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);

    return buildTokenResponse(stored.getUser());
  }

  @Transactional
  public void logout(Long userId) {
    log.info("Revoking all refresh tokens for userId={}", userId);
    refreshTokenRepository.revokeAllByUserId(userId);
  }

  // ── Helpers ───────────────────────────────────────────────

  private TokenResponse buildTokenResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = issueRefreshToken(user);

    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtService.getAccessTokenExpiryMs() / 1000)
        .build();
  }

  private String issueRefreshToken(User user) {
    String raw = UUID.randomUUID().toString();
    String hash = sha256Hex(raw);

    RefreshToken rt =
        RefreshToken.builder()
            .user(user)
            .tokenHash(hash)
            .issuedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(refreshExpiryDays))
            .build();

    refreshTokenRepository.save(rt);
    return raw;
  }

  static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 unavailable", e);
    }
  }
}
