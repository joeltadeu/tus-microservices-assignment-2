package com.pms.models.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientAppointment {

  @Schema(description = "Patient id", name = "id", example = "12")
  private Long id;

  @Schema(description = "Patient first name", name = "firstName", example = "John")
  private String firstName;

  @Schema(description = "Patient last name", name = "lastName", example = "Foreman")
  private String lastName;

  @Schema(description = "Patient email", name = "email", example = "john.foreman@gmail.com")
  private String email;

  /**
   * Returns {@code true} when this object was populated from a live response. Used by the mapper to
   * decide whether to embed the full object or fall back to the raw id field.
   */
  @Schema(hidden = true)
  public boolean isEnriched() {
    return firstName != null;
  }

  /** Factory for a degraded/fallback instance that carries only the id. */
  public static PatientAppointment idOnly(Long id) {
    return PatientAppointment.builder().id(id).build();
  }
}
