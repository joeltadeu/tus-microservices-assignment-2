package com.pms.auth.controller;

import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.TokenResponse;
import com.pms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Login, token refresh and logout")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Login — returns access token + refresh token")
  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
    log.info("Login attempt for email={}", request.getEmail());
    return ResponseEntity.ok(authService.login(request));
  }
}
