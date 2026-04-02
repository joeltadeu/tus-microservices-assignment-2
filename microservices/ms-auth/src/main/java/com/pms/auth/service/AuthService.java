package com.pms.auth.service;

import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.TokenResponse;
import com.pms.auth.model.User;
import com.pms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @Value("${pms.jwt.refresh-token-expiry-days:7}")
  private int refreshExpiryDays;

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

    return buildTokenResponse(user);
  }

  // ── Helpers ───────────────────────────────────────────────

  private TokenResponse buildTokenResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user);

    return TokenResponse.builder()
        .accessToken(accessToken)
        .tokenType("Bearer")
        .expiresIn(jwtService.getAccessTokenExpiryMs() / 1000)
        .build();
  }
}
