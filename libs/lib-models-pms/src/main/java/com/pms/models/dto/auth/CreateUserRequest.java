package com.pms.models.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

  @NotBlank
  @Email
  @Schema(
      description = "Email address — used as login credential",
      example = "dr.john.smith@pms.ie")
  private String email;

  /**
   * Password rules: min 8 chars, at least one uppercase, one digit, one special character.
   * Validated here so we never store a weak hash.
   */
  @NotBlank
  @Size(min = 8, message = "Password must be at least 8 characters")
  @Pattern(
      regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
      message =
          "Password must contain at least one uppercase letter, one digit and one special character (@$!%*?&)")
  @Schema(description = "Initial password", example = "Doctor@1234!")
  private String password;

  @NotNull
  @Schema(
      description =
          "Role to assign. Accepted values: ROLE_DOCTOR, ROLE_PATIENT, ROLE_HEALTHCARE_ADMIN",
      example = "ROLE_DOCTOR")
  private String role;

  @Schema(
      description =
          "Id of the linked domain entity (doctor.id or patient.id). Null for admin accounts.",
      example = "1")
  private Long domainId;
}
