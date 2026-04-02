package com.pms.models.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {

  @Schema(description = "Generated user id in ms-auth", example = "4")
  private Long id;

  @Schema(example = "dr.john.smith@pms.ie")
  private String email;

  @Schema(example = "ROLE_DOCTOR")
  private String role;

  @Schema(description = "Linked domain entity id (doctor.id or patient.id)", example = "3")
  private Long domainId;
}
