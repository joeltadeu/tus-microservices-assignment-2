package com.pms.auth.controller;

import com.pms.auth.service.UserService;
import com.pms.models.dto.auth.CreateUserRequest;
import com.pms.models.dto.auth.CreateUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "User Management",
    description = "Create and manage auth accounts — HEALTHCARE_ADMIN only")
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "Create a user account",
      description =
          "Creates an auth account for a doctor or patient after their domain record has been saved. "
              + "Restricted to HEALTHCARE_ADMIN.",
      security = @SecurityRequirement(name = "Authorization"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "User account created"),
        @ApiResponse(responseCode = "400", description = "Invalid request or email already taken"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden — HEALTHCARE_ADMIN role required")
      })
  @PostMapping
  @PreAuthorize("hasRole('HEALTHCARE_ADMIN')")
  public ResponseEntity<CreateUserResponse> create(@RequestBody @Valid CreateUserRequest request) {
    log.info("Create user request for email={}, role={}", request.getEmail(), request.getRole());
    var response = userService.create(request);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @Operation(
      summary = "Disable a user account by domain id",
      description =
          "Soft-disables the auth account linked to a domain entity (doctor or patient). "
              + "Called internally when a domain record is deleted. Restricted to HEALTHCARE_ADMIN.",
      security = @SecurityRequirement(name = "Authorization"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Account disabled (or not found — idempotent)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden — HEALTHCARE_ADMIN role required")
      })
  @DeleteMapping("/domain/{domainId}")
  @PreAuthorize("hasRole('HEALTHCARE_ADMIN')")
  public ResponseEntity<Void> disableByDomainId(
      @PathVariable Long domainId, @RequestParam String role) {
    log.info("Disable user request for domainId={}, role={}", domainId, role);
    userService.disableByDomainId(domainId, role);
    return ResponseEntity.noContent().build();
  }
}
