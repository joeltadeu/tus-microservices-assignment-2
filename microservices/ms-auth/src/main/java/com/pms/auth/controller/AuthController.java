package com.pms.auth.controller;

import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.RefreshRequest;
import com.pms.auth.dto.TokenResponse;
import com.pms.auth.service.AuthService;
import com.pms.auth.service.JwtService;
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
    private final JwtService  jwtService;

    @Operation(summary = "Login — returns access token + refresh token")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh — exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout — revokes all active refresh tokens for the caller")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token  = authHeader.replace("Bearer ", "");
        Long   userId = Long.parseLong(jwtService.validateAndParse(token).getSubject());
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
